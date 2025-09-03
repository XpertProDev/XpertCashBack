package com.xpertcash.service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.xpertcash.repository.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.xpertcash.DTOs.FactureDTO;
import com.xpertcash.DTOs.ProduitDTO;
import com.xpertcash.DTOs.ProduitEntreprisePaginatedResponseDTO;
import com.xpertcash.DTOs.ProduitStockPaginatedResponseDTO;
import com.xpertcash.DTOs.StockHistoryDTO;
import com.xpertcash.DTOs.PRODUIT.ProduitRequest;
import com.xpertcash.configuration.CentralAccess;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Categorie;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Facture;
import com.xpertcash.entity.FactureProduit;
import com.xpertcash.entity.Fournisseur;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.StockHistory;
import com.xpertcash.entity.StockProduitFournisseur;
import com.xpertcash.entity.Unite;
import com.xpertcash.entity.User;
import com.xpertcash.entity.UserBoutique;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.TypeProduit;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.transaction.annotation.Transactional;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import com.xpertcash.service.IMAGES.ImageStorageService;
import com.xpertcash.service.AuthenticationHelper;





@Service
public class ProduitService {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private BoutiqueRepository boutiqueRepository;



    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private UniteRepository uniteRepository;

    @Autowired
    private StockHistoryRepository stockHistoryRepository;
    @Autowired
    private FactureRepository factureRepository;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private StockProduitFournisseurRepository stockProduitFournisseurRepository;

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private LigneFactureReelleRepository ligneFactureReelleRepository;

    @Autowired
    private LigneFactureProformaRepository ligneFactureProformaRepository;

    @Autowired
    private UserBoutiqueRepository userBoutiqueRepository;




    // Créer un nouveau produit dans plusieurs boutiques
    public List<ProduitDTO> createProduit(HttpServletRequest request, List<Long> boutiqueIds,
                                      List<Integer> quantites, List<Integer> seuilAlert, ProduitRequest produitRequest, boolean addToStock, String image) {
            // ✅ Extraction et validation du token
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour créer des produits.");
        }

        List<ProduitDTO> produitsCreated = new ArrayList<>();
        
        // Créer le produit dans chaque boutique spécifiée
        for (int i = 0; i < boutiqueIds.size(); i++) {
            Long boutiqueId = boutiqueIds.get(i);
            Integer quantite = quantites.get(i);
            Integer seuil = seuilAlert.get(i);
            
            // Vérifier la boutique
            Boutique boutique = boutiqueRepository.findById(boutiqueId)
                    .orElseThrow(() -> new RuntimeException("Boutique introuvable: " + boutiqueId));
            
            // Vérifier l'appartenance à l'entreprise
            if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
                throw new RuntimeException("Accès interdit : cette boutique ne vous appartient pas");
            }
            
            // Créer le produit
            Produit produit = createSingleProduit(produitRequest, boutique, quantite, seuil, addToStock, image);
            
            // Convertir en DTO
            ProduitDTO dto = convertToProduitDTO(produit);
            produitsCreated.add(dto);
        }
        
        return produitsCreated;
    }
    
    // Méthode helper pour créer un seul produit
    private Produit createSingleProduit(ProduitRequest produitRequest, Boutique boutique, Integer quantite, Integer seuilAlert, boolean addToStock, String image) {
        // Générer code générique unique basé sur le nom du produit
        String codeGenerique = "PROD_" + System.currentTimeMillis();
        
        // Créer le produit
        Produit produit = new Produit();
        produit.setNom(produitRequest.getNom());
        produit.setDescription(produitRequest.getDescription());
        produit.setPrixVente(produitRequest.getPrixVente());
        produit.setPrixAchat(produitRequest.getPrixAchat());
        produit.setQuantite(quantite != null ? quantite : 0);
        produit.setSeuilAlert(seuilAlert != null ? seuilAlert : 0);
        produit.setCodeBare(produitRequest.getCodeBare());
        produit.setCodeGenerique(codeGenerique);
        produit.setBoutique(boutique);
        produit.setEnStock(addToStock);
        produit.setCreatedAt(LocalDateTime.now());
        produit.setLastUpdated(LocalDateTime.now());
        produit.setDeleted(false);
        
        // Gérer la catégorie
        if (produitRequest.getCategorieId() != null) {
            Categorie categorie = categorieRepository.findById(produitRequest.getCategorieId())
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            produit.setCategorie(categorie);
        }
        
        // Gérer l'unité
        if (produitRequest.getUniteId() != null) {
            Unite unite = uniteRepository.findById(produitRequest.getUniteId())
                    .orElseThrow(() -> new RuntimeException("Unité non trouvée"));
            produit.setUniteDeMesure(unite);
        }
        
        // Gérer l'image si fournie
        if (image != null && !image.isEmpty()) {
            produit.setPhoto(image);
        }
        
        // Type de produit
        if (produitRequest.getTypeProduit() != null) {
            produit.setTypeProduit(produitRequest.getTypeProduit());
        }
        
        // Date de préemption
        if (produitRequest.getDatePreemption() != null) {
            produit.setDatePreemption(produitRequest.getDatePreemption());
        }
        
        // Sauvegarder le produit
        produit = produitRepository.save(produit);
        
        // Créer le stock si nécessaire
        if (addToStock) {
            Stock stock = new Stock();
            stock.setProduit(produit);
            stock.setBoutique(boutique);
            stock.setStockActuel(quantite != null ? quantite : 0);
            stock.setSeuilAlert(seuilAlert != null ? seuilAlert : 0);
            stock.setCreatedAt(LocalDateTime.now());
            stock.setLastUpdated(LocalDateTime.now());
            stockRepository.save(stock);
        }
        
        return produit;
    }

    //Methode pour ajuster la quantiter du produit en stock
    public Facture ajouterStock(Long boutiqueId, Map<Long, Integer> produitsQuantites, String description, String codeFournisseur, Long fournisseurId, HttpServletRequest request) {

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        // ✅ Récupération de la boutique
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        Long entrepriseId = boutique.getEntreprise().getId();

        // 🔒 Vérifier que l'utilisateur appartient bien à la même entreprise
        if (!user.getEntreprise().getId().equals(entrepriseId)) {
            throw new RuntimeException("Accès interdit : cette boutique ne vous appartient pas");
        }

        // 🔐 Contrôle d'accès strict : seulement ADMIN ou permission explicite
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        boolean hasPermissionGestion = user.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK);


        if (!isAdminOrManager && !hasPermission && !hasPermissionGestion) {
            throw new RuntimeException("Accès refusé : seuls les ADMIN ou les utilisateurs ayant la permission GERER_PRODUITS peuvent gérer le stock de cette boutique.");
        }



        List<Produit> produits = new ArrayList<>();
        Fournisseur fournisseurEntity = null;

       for (Map.Entry<Long, Integer> entry : produitsQuantites.entrySet()) {
        Long produitId = entry.getKey();
        Integer quantiteAjoute = entry.getValue();

        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

          // 🔒 Vérifier que le produit appartient à la même entreprise (via sa boutique)
        Boutique produitBoutique = produit.getBoutique();
        if (produitBoutique == null || !produitBoutique.getEntreprise().getId().equals(entrepriseId)) {
            throw new RuntimeException("Le produit ID " + produitId + " n'appartient pas à l'entreprise de la boutique.");
        }

        Stock stock = stockRepository.findByProduit(produit);
        if (stock == null) {
            throw new RuntimeException("Stock introuvable pour ce produit");
        }

    int stockAvant = stock.getStockActuel();
    int nouvelleQuantiteProduit = produit.getQuantite() + quantiteAjoute;

    // Mettre à jour produit et stock
    produit.setQuantite(nouvelleQuantiteProduit);
    produitRepository.save(produit);

    stock.setStockActuel(nouvelleQuantiteProduit);
    stock.setQuantiteAjoute(quantiteAjoute);
    stock.setStockApres(nouvelleQuantiteProduit);
    stock.setLastUpdated(LocalDateTime.now());


    // Initialiser la liste si null

    stockRepository.save(stock);

    // Charger le fournisseur seulement s'il est fourni
    if (fournisseurId != null) {
        fournisseurEntity = fournisseurRepository.findById(fournisseurId)
            .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé avec l'ID : " + fournisseurId));
    }

    // 🔁 Enregistrer dans StockProduitFournisseur
    StockProduitFournisseur spf = new StockProduitFournisseur();
    spf.setStock(stock);
    spf.setProduit(produit);
    spf.setQuantiteAjoutee(quantiteAjoute);

    // Affecter le fournisseur uniquement s'il existe
    if (fournisseurEntity != null) {
        spf.setFournisseur(fournisseurEntity);
    }

    stockProduitFournisseurRepository.save(spf);



    // 🕒 Historique
    StockHistory stockHistory = new StockHistory();
    stockHistory.setAction("Ajout sur quantité");
    stockHistory.setQuantite(quantiteAjoute);
    stockHistory.setStockAvant(stockAvant);
    stockHistory.setStockApres(nouvelleQuantiteProduit);
    stockHistory.setDescription(description);
    stockHistory.setCreatedAt(LocalDateTime.now());
    stockHistory.setStock(stock);
    stockHistory.setUser(user);
    if (codeFournisseur != null && !codeFournisseur.isEmpty()) {
        stockHistory.setCodeFournisseur(codeFournisseur);
    }

    if (fournisseurId != null) {
        stockHistory.setFournisseur(fournisseurEntity);
    }

    stockHistoryRepository.save(stockHistory);

    produits.add(produit);
}

        // Enregistrer une facture avec plusieurs produits
        return enregistrerFacture("AJOUTER", produits, produitsQuantites, description, codeFournisseur, fournisseurEntity, user);
    }

    // Méthode pour ajuster la quantité du produit en stock (retirer des produits)
    public FactureDTO retirerStock(Long boutiqueId, Map<Long, Integer> produitsQuantites, String description, HttpServletRequest request) {

        // 🔐 Extraction et vérification du token JWT
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        // 3️⃣ Vérification de la boutique
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        if (!boutique.isActif()) {
            throw new RuntimeException("La boutique est désactivée, opération non autorisée.");
        }

        // 4️⃣ Vérification que la boutique appartient à l'entreprise de l'utilisateur
        Long entrepriseId = boutique.getEntreprise().getId();
        if (!entrepriseId.equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        // 5️⃣ Vérification stricte des droits : seul ADMIN ou permission explicite
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        boolean hasPermissionGestion = user.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK);
        

        if (!isAdminOrManager && !hasPermission && !hasPermissionGestion) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour retirer du stock !");
        }



    List<Produit> produits = new ArrayList<>();

    // 6️⃣ Traitement des produits
    for (Map.Entry<Long, Integer> entry : produitsQuantites.entrySet()) {
        Long produitId = entry.getKey();
        Integer quantiteRetirer = entry.getValue();

        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        // Vérification que le produit appartient à la même entreprise que la boutique
        if (!produit.getBoutique().getEntreprise().getId().equals(entrepriseId)) {
            throw new RuntimeException("Produit ID " + produitId + " n'appartient pas à l'entreprise de la boutique.");
        }

        Stock stock = stockRepository.findByProduit(produit);
        if (stock == null) {
            throw new RuntimeException("Stock introuvable pour ce produit");
        }

        int stockAvant = stock.getStockActuel();
        if (quantiteRetirer > stockAvant) {
            throw new RuntimeException("Impossible de retirer plus que le stock disponible (" + stockAvant + ").");
        }

        int nouvelleQuantiteProduit = produit.getQuantite() - quantiteRetirer;

        // Mise à jour produit + stock
        produit.setQuantite(nouvelleQuantiteProduit);
        produitRepository.save(produit);

        stock.setStockActuel(nouvelleQuantiteProduit);
        stock.setQuantiteRetirer(quantiteRetirer);
        stock.setStockApres(nouvelleQuantiteProduit);
        stock.setLastUpdated(LocalDateTime.now());
        stockRepository.save(stock);

        // 🕒 Historique du stock
        StockHistory stockHistory = new StockHistory();
        stockHistory.setAction("Réduction sur quantité");
        stockHistory.setQuantite(quantiteRetirer);
        stockHistory.setStockAvant(stockAvant);
        stockHistory.setStockApres(nouvelleQuantiteProduit);
        stockHistory.setDescription(description);
        stockHistory.setCreatedAt(LocalDateTime.now());
        stockHistory.setStock(stock);
        stockHistory.setUser(user);
        stockHistoryRepository.save(stockHistory);

        produits.add(produit);
    }

    // 7️⃣ Génération de la facture
    Facture facture = enregistrerFacture("Réduction", produits, produitsQuantites, description, null, null, user);
    return new FactureDTO(facture);
}




      // Génère un numéro unique de facture
   private String generateNumeroFacture() {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        // Récupérer les factures de l’année en cours (à adapter si besoin)
        List<Facture> facturesAnnee = factureRepository.findByYear(currentYear);

        // Trouver le plus grand numéro de facture pour cette année
        int lastNumero = facturesAnnee.stream()
            .map(f -> extraireNumero(f.getNumeroFacture()))
            .max(Integer::compareTo)
            .orElse(0);

        int newNumero = lastNumero + 1;

        return String.format("FAC-%04d-%02d-%d", newNumero, currentMonth, currentYear);
    }

    private int extraireNumero(String numeroFacture) {
        try {
            String[] parts = numeroFacture.split("-");
            return Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return 0; // en cas d'erreur de parsing
        }
    }


    // Méthode pour enregistrer une facture
   public Facture enregistrerFacture(String type, List<Produit> produits, Map<Long, Integer> quantites,
                                  String description, String codeFournisseur, Fournisseur fournisseur, User user) {
    Facture facture = new Facture();
    facture.setNumeroFacture(generateNumeroFacture());
    facture.setType(type);
    facture.setDescription(description);
    facture.setDateFacture(LocalDateTime.now());
    facture.setUser(user);

    // Associer la boutique à la facture
    if (!produits.isEmpty() && produits.get(0).getBoutique() != null) {
        facture.setBoutique(produits.get(0).getBoutique());
    } else {
        throw new RuntimeException("Impossible de créer une facture sans boutique associée.");
    }

    List<FactureProduit> factureProduits = new ArrayList<>();
    for (Produit produit : produits) {
        FactureProduit factureProduit = new FactureProduit();
        factureProduit.setFacture(facture);
        factureProduit.setProduit(produit);
        factureProduit.setQuantite(quantites.get(produit.getId()));
        factureProduit.setPrixUnitaire(produit.getPrixVente());
        factureProduit.setTotal(factureProduit.getQuantite() * factureProduit.getPrixUnitaire());
        factureProduits.add(factureProduit);
    }

    facture.setFactureProduits(factureProduits);

    if (codeFournisseur != null && !codeFournisseur.isEmpty()) {
        facture.setCodeFournisseur(codeFournisseur);
    }

    // Fournisseur requis seulement pour certaines opérations
    if ("Ajout".equalsIgnoreCase(type) || "Approvisionnement".equalsIgnoreCase(type)) {
        if (fournisseur == null) {
            throw new RuntimeException("Le fournisseur est requis pour une facture de type '" + type + "'");
        }

        Fournisseur fournisseurEntity = fournisseurRepository.findById(fournisseur.getId())
            .orElseThrow(() -> new RuntimeException("Fournisseur introuvable"));
        facture.setFournisseur(fournisseurEntity);
    } else if (fournisseur != null) {
        // Cas facultatif : on le récupère s'il est présent, sinon on l'ignore
        Fournisseur fournisseurEntity = fournisseurRepository.findById(fournisseur.getId())
            .orElseThrow(() -> new RuntimeException("Fournisseur introuvable"));
        facture.setFournisseur(fournisseurEntity);
    }

    return factureRepository.save(facture);
}

     //Methode liste Historique sur Stock
  public List<StockHistoryDTO> getStockHistory(Long produitId, HttpServletRequest request) {

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    if (user.getEntreprise() == null) {
        throw new RuntimeException("Utilisateur non rattaché à une entreprise.");
    }

    // 🔐 Vérification du rôle ou des permissions
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS)
                            || user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);

    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour consulter l'historique de stock.");
    }

    // 🔍 Vérification de l'existence du produit
    Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'ID : " + produitId));

    // 🔐 Vérification d'appartenance du produit à l'entreprise de l'utilisateur
    Boutique boutique = produit.getBoutique();
    if (boutique == null || boutique.getEntreprise() == null) {
        throw new RuntimeException("Le produit ou sa boutique ne sont pas liés à une entreprise.");
    }

    Long entrepriseProduitId = boutique.getEntreprise().getId();
    Long entrepriseUserId = user.getEntreprise().getId();

    if (!entrepriseProduitId.equals(entrepriseUserId)) {
        throw new RuntimeException("Accès interdit : ce produit n'appartient pas à votre entreprise !");
    }

    // 🔍 Vérification que le stock existe
    Stock stock = stockRepository.findByProduit(produit);
    if (stock == null) {
        throw new RuntimeException("Aucun stock associé à ce produit.");
    }

    // 📦 Récupération de l'historique du stock
    List<StockHistory> stockHistories = stockHistoryRepository.findByStock(stock);
    if (stockHistories.isEmpty()) {
        throw new RuntimeException("Aucun historique de stock trouvé pour ce produit.");
    }

    // 🛠 Mapping vers DTO
    return stockHistories.stream()
            .map(stockHistory -> {
                StockHistoryDTO dto = new StockHistoryDTO();
                dto.setId(stockHistory.getId());
                dto.setAction(stockHistory.getAction());
                dto.setQuantite(stockHistory.getQuantite());
                dto.setStockAvant(stockHistory.getStockAvant());
                dto.setStockApres(stockHistory.getStockApres());
                dto.setDescription(stockHistory.getDescription());
                dto.setCreatedAt(stockHistory.getCreatedAt());

                User historiqueUser = stockHistory.getUser();
                if (historiqueUser != null) {
                    dto.setNomComplet(historiqueUser.getNomComplet());
                    dto.setPhone(historiqueUser.getPhone());
                    if (historiqueUser.getRole() != null) {
                        dto.setRole(historiqueUser.getRole().getName());
                    }
                }

                return dto;
            })
            .collect(Collectors.toList());
}


    // Récupérer tous les mouvements de stock
    public List<StockHistoryDTO> getAllStockHistory(HttpServletRequest request) {

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    if (user.getEntreprise() == null) {
        throw new RuntimeException("Utilisateur non rattaché à une entreprise.");
    }

    Long entrepriseId = user.getEntreprise().getId();

    // 🔍 Récupérer tous les historiques filtrés par entreprise
    List<StockHistory> stockHistories = stockHistoryRepository.findAll()
        .stream()
        .filter(history -> {
            Stock stock = history.getStock();
            Produit produit = (stock != null) ? stock.getProduit() : null;
            Boutique boutique = (produit != null) ? produit.getBoutique() : null;
            Entreprise entreprise = (boutique != null) ? boutique.getEntreprise() : null;
            return entreprise != null && entrepriseId.equals(entreprise.getId());
        })
        .collect(Collectors.toList());

    // 🛠 Mapping en DTO
    return stockHistories.stream()
            .map(stockHistory -> {
                StockHistoryDTO dto = new StockHistoryDTO();
                dto.setId(stockHistory.getId());
                dto.setAction(stockHistory.getAction());
                dto.setQuantite(stockHistory.getQuantite());
                dto.setStockAvant(stockHistory.getStockAvant());
                dto.setStockApres(stockHistory.getStockApres());
                dto.setDescription(stockHistory.getDescription());
                dto.setCreatedAt(stockHistory.getCreatedAt());

                User historiqueUser = stockHistory.getUser();
                if (historiqueUser != null) {
                    dto.setNomComplet(historiqueUser.getNomComplet());
                    dto.setPhone(historiqueUser.getPhone());
                    if (historiqueUser.getRole() != null) {
                        dto.setRole(historiqueUser.getRole().getName());
                    }
                }

                return dto;
            })
            .collect(Collectors.toList());
}


   //Lister Stock
    public List<Stock> getAllStocks(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        if (user.getEntreprise() == null) {
            throw new RuntimeException("Utilisateur non rattaché à une entreprise.");
        }

        Long entrepriseId = user.getEntreprise().getId();

        // 📦 Récupérer tous les stocks liés à cette entreprise
        return stockRepository.findAll().stream()
                .filter(stock -> {
                    Produit produit = stock.getProduit();
                    Boutique boutique = (produit != null) ? produit.getBoutique() : null;
                    Entreprise entreprise = (boutique != null) ? boutique.getEntreprise() : null;
                    return entreprise != null && entreprise.getId().equals(entrepriseId);
                })
                .collect(Collectors.toList());
    }



   // Update Produit
    public ProduitDTO updateProduct(Long produitId, ProduitRequest produitRequest, MultipartFile imageFile, boolean addToStock, HttpServletRequest request)
 {
    User admin = authHelper.getAuthenticatedUserWithFallback(request);

    // 🛡️ Autorisation et permission
    RoleType role = admin.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = admin.getRole().hasPermission(PermissionType.GERER_PRODUITS);

    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour modifier un produit !");
    }

    Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

    // ✅ Vérifier que l'admin et le produit sont dans la même entreprise
    Long produitEntrepriseId = produit.getBoutique().getEntreprise().getId();
    Long adminEntrepriseId = admin.getEntreprise().getId();
    if (!produitEntrepriseId.equals(adminEntrepriseId)) {
        throw new RuntimeException("Accès interdit : ce produit n'appartient pas à votre entreprise.");
    }

    // Mise à jour des informations du produit
    if (produitRequest.getNom() != null) produit.setNom(produitRequest.getNom());
    if (produitRequest.getDescription() != null) produit.setDescription(produitRequest.getDescription());
    if (produitRequest.getPrixVente() != null) produit.setPrixVente(produitRequest.getPrixVente());
    if (produitRequest.getPrixAchat() != null) produit.setPrixAchat(produitRequest.getPrixAchat());
    if (produitRequest.getQuantite() != null) produit.setQuantite(produitRequest.getQuantite());
    if (produitRequest.getSeuilAlert() != null) produit.setSeuilAlert(produitRequest.getSeuilAlert());
    if (produitRequest.getCodeBare() != null) produit.setCodeBare(produitRequest.getCodeBare());

    if (produitRequest.getTypeProduit() != null) {
    produit.setTypeProduit(produitRequest.getTypeProduit());
    }



    if (imageFile != null && !imageFile.isEmpty()) {
        // Supprimer l'ancienne image
        if (produit.getPhoto() != null && !produit.getPhoto().isBlank()) {
            String oldPhotoPathStr = "src/main/resources/static" + produit.getPhoto();
            Path oldPhotoPath = Paths.get(oldPhotoPathStr);
            try {
                Files.deleteIfExists(oldPhotoPath);
                System.out.println("🗑 Ancienne photo supprimée : " + oldPhotoPathStr);
            } catch (IOException e) {
                System.err.println("⚠️ Erreur lors de la suppression de l'ancienne photo : " + e.getMessage());
            }
        }

        // Enregistrement de la nouvelle image
        String newPhotoPath = imageStorageService.saveImage(imageFile); // stocke et retourne le chemin
        produit.setPhoto(newPhotoPath);
    }



    // Mise à jour de la catégorie si nécessaire
    if (produitRequest.getCategorieId() != null) {
        Categorie categorie = categorieRepository.findById(produitRequest.getCategorieId())
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
        produit.setCategorie(categorie);
    }

    // Mise à jour de l'unité si nécessaire
    if (produitRequest.getUniteId() != null) {
        Unite unite = uniteRepository.findById(produitRequest.getUniteId())
                .orElseThrow(() -> new RuntimeException("Unité de mesure non trouvée"));
        produit.setUniteDeMesure(unite);
    }

    // Sauvegarde des modifications du produit
    produitRepository.save(produit);

    // Gestion du stock : ajout ou suppression en fonction de addToStock
    if (addToStock) {
        // Recherche du stock existant pour ce produit
        Stock stock = stockRepository.findByProduit(produit);

        if (stock == null) {
            // Si le stock n'existe pas, création d'un nouveau stock
            Stock newStock = new Stock();
            newStock.setProduit(produit);  // Associer le produit au stock
            newStock.setStockActuel(produit.getQuantite() != null ? produit.getQuantite() : 0);
            newStock.setBoutique(produit.getBoutique());
            newStock.setCreatedAt(LocalDateTime.now());
            newStock.setLastUpdated(LocalDateTime.now());

            // Ajouter le seuil d'alerte dans le stock
            if (produitRequest.getSeuilAlert() != null) {
                newStock.setSeuilAlert(produitRequest.getSeuilAlert());
            } else {
                newStock.setSeuilAlert(produit.getSeuilAlert());
            }

            stockRepository.save(newStock);
        } else {
            // Si le stock existe déjà, mise à jour des informations du stock
            stock.setStockActuel(produit.getQuantite() != null ? produit.getQuantite() : 0);

            stock.setQuantiteAjoute(0);
            stock.setQuantiteRetirer(0);


            // Mettre à jour le seuil d'alerte si nécessaire
            if (produitRequest.getSeuilAlert() != null) {
                stock.setSeuilAlert(produitRequest.getSeuilAlert());
            }

            stock.setLastUpdated(LocalDateTime.now());
            stockRepository.save(stock);
        }

        produit.setEnStock(true);
    } else {
        // Si le produit ne doit plus être en stock, suppression du stock
        Stock stock = stockRepository.findByProduit(produit);

        if (stock != null) {
            // Supprimer d'abord tous les historiques liés
            List<StockHistory> historyRecords = stockHistoryRepository.findByStock(stock);
            if (!historyRecords.isEmpty()) {
                stockHistoryRepository.deleteAll(historyRecords);
            }

            // Supprimer ensuite le stock
            stockRepository.delete(stock);
        }



        produit.setEnStock(false);
    }

    produitRepository.save(produit);

    // Mapper Produit vers ProduitDTO pour la réponse
    ProduitDTO produitDTO = new ProduitDTO();
    produitDTO.setId(produit.getId());
    produitDTO.setNom(produit.getNom());
    produitDTO.setPrixVente(produit.getPrixVente());
    produitDTO.setPrixAchat(produit.getPrixAchat());
    produitDTO.setQuantite(produit.getQuantite());
    produitDTO.setSeuilAlert(produit.getSeuilAlert());
    produitDTO.setCategorieId(produit.getCategorie() != null ? produit.getCategorie().getId() : null);
    produitDTO.setUniteId(produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getId() : null);
    produitDTO.setPhoto(produit.getPhoto());
    produitDTO.setDescription(produit.getDescription());
    produitDTO.setCodeBare(produit.getCodeBare());
    produitDTO.setEnStock(produit.getEnStock());
    produitDTO.setCreatedAt(produit.getCreatedAt());
    produitDTO.setLastUpdated(produit.getLastUpdated());
    produitDTO.setNomCategorie(produit.getCategorie() != null ? produit.getCategorie().getNom() : null);
    produitDTO.setNomUnite(produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getNom() : null);
    produitDTO.setTypeProduit(produit.getTypeProduit() != null ? produit.getTypeProduit().name() : null);





    return produitDTO;
}

    // Méthode pour "supprimer" (mettre dans la corbeille) le produit s'il n'est pas en stock
     @Transactional
    public void corbeille(Long produitId, HttpServletRequest request) {
        // 1. Vérification du produit
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        // 2. Vérification du token JWT
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        String token = authHeader.substring(7);
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        // ✅ 4. Vérification d'appartenance à la même entreprise
        Long produitEntrepriseId = produit.getBoutique().getEntreprise().getId();
        Long userEntrepriseId = user.getEntreprise().getId();
        if (!produitEntrepriseId.equals(userEntrepriseId)) {
            throw new RuntimeException("Action interdite : ce produit n'appartient pas à votre entreprise.");
        }

        // 🔐 5. Vérification des permissions
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Action non autorisée : permissions insuffisantes");
        }

        // 🚫 6. Validation métier
        if (produit.getEnStock()) {
            throw new RuntimeException("⚠️ Impossible de supprimer le produit car il est encore en stock");
        }

        // 🚫 7. Validation métier : lié à des factures ?
        boolean produitUtilise = ligneFactureReelleRepository.existsByProduitId(produitId);
        boolean produitUtiliseProforma = ligneFactureProformaRepository.existsByProduitId(produitId);
        
        if (produitUtilise || produitUtiliseProforma) {
            throw new RuntimeException("⚠️ Impossible de supprimer le produit car il est lié à des factures");
        }
        
        
 
        // 🗑️ 8. Marquage comme supprimé
        produit.setDeleted(true);
        produit.setDeletedAt(LocalDateTime.now());
        produit.setDeletedBy(user.getId());
        produitRepository.save(produit);
    }

    //Methoce Supprimer uniquement le stock
    public void deleteStock(Long produitId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        if (!produit.getEnStock()) {
            throw new RuntimeException("Le produit n'est pas en stock, suppression du stock impossible !");
        }

        Stock stock = stockRepository.findByProduit(produit);
        if (stock != null) {
            stockRepository.delete(stock);
            produit.setEnStock(false);
            produitRepository.save(produit);
            System.out.println("✅ Stock supprimé et produit marqué comme 'hors stock'");
        } else {
            throw new RuntimeException("Aucun stock trouvé pour ce produit !");
        }
    }

    // Méthode pour restaurer un ou plusieurs produit depuis la corbeille
    @Transactional
    public void restaurerProduitsDansBoutique(Long boutiqueId, List<Long> produitIds, HttpServletRequest request) {

        // Vérifications habituelles (token, user, permissions)
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        String token = authHeader.substring(7);
        User user = authHelper.getAuthenticatedUserWithFallback(request);

         // Vérification des rôles et permissions
         RoleType role = user.getRole().getName();
      boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
       boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);


       if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Action non autorisée : permissions insuffisantes");
        }

        // Vérification boutique
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        for (Long produitId : produitIds) {
            Produit produit = produitRepository.findById(produitId)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé : ID " + produitId));

            // Vérifier que le produit appartient bien à la boutique
            if (!produit.getBoutique().getId().equals(boutiqueId)) {
                throw new RuntimeException("Le produit ID " + produitId + " n'appartient pas à la boutique ID " + boutiqueId);
            }

            if (!produit.getDeleted()) {
                throw new RuntimeException("Le produit ID " + produitId + " n'est pas dans la corbeille");
            }

            produit.setDeleted(false);
            produit.setDeletedAt(null);
            produit.setDeletedBy(null);

            produitRepository.save(produit);
        }
    }

    //Methode pour vide Corbeille dune boutique
    @Transactional
    public void viderCorbeille(Long boutiqueId, HttpServletRequest request) {

        // 1. Vérification du token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        String token = authHeader.substring(7);
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        // 3. Vérification des rôles et permissions
        boolean isAdmin = user.getRole().getName() == RoleType.ADMIN;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAdmin && !hasPermission) {
            throw new RuntimeException("Action non autorisée : permissions insuffisantes");
        }

        // 4. Vérification de la boutique
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        // ✅ 5. Vérification d'appartenance à la même entreprise
        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Action interdite : cette boutique ne vous appartient pas");
        }

        // 6. Suppression des produits marqués comme supprimés
        List<Produit> produitsSupprimes = produitRepository.findByBoutiqueIdAndDeletedTrue(boutiqueId);
        produitRepository.deleteAll(produitsSupprimes);
    }

    // Lister Produit par boutique (excluant les produits dans la corbeille)
    @Transactional
    public List<ProduitDTO> getProduitsParStock(Long boutiqueId, HttpServletRequest request) {
        return getProduitsParStockPaginated(boutiqueId, 0, Integer.MAX_VALUE, request).getContent();
    }

    // Méthode scalable avec pagination pour récupérer les produits d'une boutique
    @Transactional
    public ProduitStockPaginatedResponseDTO getProduitsParStockPaginated(
            Long boutiqueId, 
            int page, 
            int size, 
            HttpServletRequest request) {
        
        // --- 1. Validation des paramètres de pagination ---
        if (page < 0) page = 0;
        if (size <= 0) size = 20; // Taille par défaut
        if (size > 100) size = 100; // Limite maximale pour éviter la surcharge
        
        // Validation de l'ID de la boutique
        if (boutiqueId == null) {
            throw new RuntimeException("L'ID de la boutique ne peut pas être null");
        }
        
        // --- 2. Extraction utilisateur via JWT ---
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        // --- 3. Vérification de la boutique ---
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));
        if (!boutique.isActif()) {
            throw new RuntimeException("Cette boutique est désactivée, ses produits ne sont pas accessibles !");
        }

        // --- 4. Vérification d'accès à l'entreprise ---
        Long entrepriseId = boutique.getEntreprise().getId();
        if (!entrepriseId.equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique ne vous appartient pas");
        }

        // --- 5. Vérification des droits ---
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS)
                            || user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS)
                            || user.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK)
                            || user.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès interdit : vous n'avez pas les droits pour consulter les produits.");
        }

        // --- 6. Vérification rôle vendeur ---
        boolean isVendeur = role == RoleType.VENDEUR;
        if (isVendeur) {
            Optional<UserBoutique> userBoutique = userBoutiqueRepository.findByUserIdAndBoutiqueId(user.getId(), boutiqueId);
            if (!userBoutique.isPresent()) {
                throw new RuntimeException("Vous n'êtes pas affecté à cette boutique, vous ne pouvez pas voir ses produits.");
            }
        }

        // --- 7. Créer le Pageable avec tri optimisé ---
        Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());

        // --- 8. Récupérer les produits avec pagination ---
        Page<Produit> produitsPage = produitRepository.findProduitsByBoutiqueIdPaginated(boutiqueId, pageable);

        // --- 9. Récupérer les statistiques globales (une seule fois) ---
        long totalProduitsActifs = produitRepository.countProduitsActifsByBoutiqueId(boutiqueId);
        long totalProduitsEnStock = produitRepository.countProduitsEnStockByBoutiqueId(boutiqueId);
        long totalProduitsHorsStock = produitRepository.countProduitsHorsStockByBoutiqueId(boutiqueId);

        // --- 10. Convertir les produits de la page courante vers DTOs ---
        List<ProduitDTO> produitsDTOs = produitsPage.getContent().stream()
                .map(this::convertToProduitDTO)
                .collect(Collectors.toList());

        // --- 11. Créer la page de DTOs ---
        Page<ProduitDTO> dtoPage = new PageImpl<>(
                produitsDTOs,
                pageable,
                totalProduitsActifs
        );

        // --- 12. Retourner la réponse paginée ---
        return ProduitStockPaginatedResponseDTO.fromPage(dtoPage, totalProduitsActifs, totalProduitsEnStock, totalProduitsHorsStock);
    }


   


    // Méthode pour convertir un Produit en ProduitDTO
        private ProduitDTO convertToProduitDTO(Produit produit) {
            ProduitDTO produitDTO = new ProduitDTO();
            produitDTO.setId(produit.getId());
            produitDTO.setNom(produit.getNom());
            produitDTO.setPrixVente(produit.getPrixVente());
            produitDTO.setDescription(produit.getDescription());
            produitDTO.setCodeGenerique(produit.getCodeGenerique());
            produitDTO.setPrixAchat(produit.getPrixAchat());
            produitDTO.setQuantite(produit.getQuantite());
            produitDTO.setSeuilAlert(produit.getSeuilAlert());
            produitDTO.setCategorieId(produit.getCategorie() != null ? produit.getCategorie().getId() : null);
            produitDTO.setUniteId(produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getId() : null);
            produitDTO.setCodeBare(produit.getCodeBare());
            produitDTO.setPhoto(produit.getPhoto());
            produitDTO.setEnStock(produit.getEnStock());

            produitDTO.setCreatedAt(produit.getCreatedAt());
            produitDTO.setLastUpdated(produit.getLastUpdated());


            // Récupérer et affecter le nom de la catégorie et de l'unité
            produitDTO.setNomCategorie(produit.getCategorie() != null ? produit.getCategorie().getNom() : null);
            produitDTO.setNomUnite(produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getNom() : null);
            //Type de produit
            TypeProduit type = produit.getTypeProduit();
            produitDTO.setTypeProduit(type != null ? type.name() : null);
            // Assigner l'ID de la boutique
            produitDTO.setBoutiqueId(produit.getBoutique() != null ? produit.getBoutique().getId() : null);


            return produitDTO;
        }

    // Méthode pour lister les produits dans la corbeille
        @Transactional(readOnly = true)
    public List<ProduitDTO> getProduitsDansCorbeille(Long boutiqueId, HttpServletRequest request) {
    // 1. Vérification du token JWT
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    String token = authHeader.substring(7);
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    // 3. Vérification de la boutique
    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

    if (!boutique.isActif()) {
        throw new RuntimeException("Cette boutique est désactivée !");
    }

    if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : cette boutique ne vous appartient pas");
    }

    // 4. Vérification des rôles et permissions
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasProduitPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
    boolean hasVentePermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);

    if (!isAdminOrManager && !hasProduitPermission && !hasVentePermission) {
        throw new RuntimeException("Accès interdit : vous n'avez pas les droits pour consulter la corbeille.");
    }

    // 5. Récupération des produits supprimés
    List<Produit> produitsSupprimes = produitRepository.findByBoutiqueIdAndDeletedTrue(boutiqueId);

    return produitsSupprimes.stream()
            .map(this::convertToProduitDTO)
            .collect(Collectors.toList());
}

    //Methode Total des Produit:
    public Map<String, Integer> getTotalQuantitesParStock(Long boutiqueId) {
        // Récupérer les produits en stock de la boutique
        List<Produit> produitsEnStock = produitRepository.findByBoutiqueIdAndEnStockTrue(boutiqueId);
        // Récupérer les produits non en stock de la boutique
        List<Produit> produitsNonEnStock = produitRepository.findByBoutiqueIdAndEnStockFalse(boutiqueId);

        // Calculer les quantités en stock et non en stock
        int totalEnStock = produitsEnStock.stream().mapToInt(Produit::getQuantite).sum();
        int totalNonEnStock = produitsNonEnStock.stream().mapToInt(Produit::getQuantite).sum();

        // Créer une map avec les résultats
        Map<String, Integer> totals = new HashMap<>();
        totals.put("totalEnStock", totalEnStock);
        totals.put("totalNonEnStock", totalNonEnStock);

        return totals;
    }

    //Methode pour reuperer un produit par son id
   public ProduitDTO getProduitById(Long id, HttpServletRequest request) {
    // Vérification de l'autorisation de l'admin
    User admin = authHelper.getAuthenticatedUserWithFallback(request);

    // Autorisation et permission
    RoleType role = admin.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = admin.getRole().hasPermission(PermissionType.GERER_PRODUITS)
                        || admin.getRole().hasPermission(PermissionType.VENDRE_PRODUITS)
                        || admin.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK)
                        || admin.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);



    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour consulter ce produit !");
    }

    Produit produit = produitRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

    // Vérification que le produit appartient à l'entreprise de l'utilisateur
    if (produit.getBoutique() == null || produit.getBoutique().getEntreprise() == null) {
        throw new RuntimeException("Produit ou boutique non rattaché à une entreprise !");
    }

    if (!produit.getBoutique().getEntreprise().getId().equals(admin.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : ce produit n'appartient pas à votre entreprise !");
    }

    ProduitDTO dto = mapToProduitDTO(produit);

    // Liste pour stocker les infos des boutiques
    List<Map<String, Object>> boutiquesInfo = new ArrayList<>();

    // Récupérer tous les produits ayant le même codeGenerique
    List<Produit> produitsMemeCode = produitRepository.findByCodeGenerique(produit.getCodeGenerique());

    int totalQuantite = 0;

    // Boucle pour traiter chaque produit avec le même codeGenerique
    for (Produit p : produitsMemeCode) {
        if (p.getBoutique() != null
                && p.getBoutique().isActif()
                && p.getBoutique().getEntreprise().getId().equals(admin.getEntreprise().getId())) {

            Map<String, Object> boutiqueData = new HashMap<>();
            boutiqueData.put("id", p.getBoutique().getId());
            boutiqueData.put("nom", p.getBoutique().getNomBoutique());
            boutiqueData.put("quantite", p.getQuantite());

            boutiquesInfo.add(boutiqueData);

            totalQuantite += p.getQuantite();
        }
    }

    dto.setQuantite(totalQuantite);
    dto.setBoutiques(boutiquesInfo);

    return dto;
}


    private ProduitDTO mapToProduitDTO(Produit produit) {
        ProduitDTO dto = new ProduitDTO();
        dto.setId(produit.getId());
        dto.setNom(produit.getNom());
        dto.setPrixVente(produit.getPrixVente());
        dto.setPrixAchat(produit.getPrixAchat());
        dto.setQuantite(produit.getQuantite());
        dto.setSeuilAlert(produit.getSeuilAlert());
        dto.setCategorieId(produit.getCategorie() != null ? produit.getCategorie().getId() : null);
        dto.setUniteId(produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getId() : null);
        dto.setCodeBare(produit.getCodeBare());
        dto.setCodeGenerique(produit.getCodeGenerique());
        dto.setDescription(produit.getDescription());
        dto.setPhoto(produit.getPhoto());
        dto.setTypeProduit(produit.getTypeProduit() != null ? produit.getTypeProduit().name() : null);
        dto.setEnStock(produit.getEnStock());
        dto.setCreatedAt(produit.getCreatedAt());
        dto.setLastUpdated(produit.getLastUpdated());
        dto.setNomCategorie(produit.getCategorie() != null ? produit.getCategorie().getNom() : null);
        dto.setNomUnite(produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getNom() : null);
        dto.setBoutiqueId(produit.getBoutique() != null ? produit.getBoutique().getId() : null);
        dto.setDatePreemption(produit.getDatePreemption());

        return dto;
    }

    // Méthode pour récupérer tous les produits de toutes les boutiques d'une entreprise
    public List<ProduitDTO> getProduitsParEntreprise(Long entrepriseId, HttpServletRequest request) {
        return getProduitsParEntreprisePaginated(entrepriseId, 0, Integer.MAX_VALUE, request).getContent();
    }

    // Méthode scalable avec pagination pour récupérer les produits d'une entreprise
    public ProduitEntreprisePaginatedResponseDTO getProduitsParEntreprisePaginated(
            Long entrepriseId, 
            int page, 
            int size, 
            HttpServletRequest request) {
        
        // --- 1. Validation des paramètres de pagination ---
        if (page < 0) page = 0;
        if (size <= 0) size = 20; // Taille par défaut
        if (size > 100) size = 100; // Limite maximale pour éviter la surcharge
        
        // Validation de l'ID de l'entreprise
        if (entrepriseId == null) {
            throw new RuntimeException("L'ID de l'entreprise ne peut pas être null");
        }
        
        // --- 2. Extraire l'utilisateur via JWT ---
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        if (user.getEntreprise() == null) {
            throw new RuntimeException("Aucune entreprise associée à l'utilisateur ID: " + user.getId());
        }
        
        Entreprise entreprise = user.getEntreprise();
        if (entreprise.getId() == null) {
            throw new RuntimeException("L'ID de l'entreprise de l'utilisateur est null");
        }

        // --- 3. Vérification des droits ---
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermissionGestionProduits = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        boolean hasPermissionGestionFacturation = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

        if (!isAdminOrManager && !hasPermissionGestionProduits && !hasPermissionGestionFacturation) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires pour consulter les produits.");
        }

        // --- 4. Vérification de l'entreprise ---
        if (!entreprise.getId().equals(entrepriseId)) {
            throw new RuntimeException("Accès refusé : vous ne pouvez pas accéder aux produits d'une autre entreprise. Votre entreprise: " + entreprise.getId() + ", Demandée: " + entrepriseId);
        }

        // --- 5. Créer le Pageable avec tri optimisé ---
        Pageable pageable = PageRequest.of(page, size, Sort.by("codeGenerique").ascending().and(Sort.by("nom").ascending()));

        // --- 6. Récupérer les produits avec pagination ---
        Page<Produit> produitsPage = produitRepository.findProduitsByEntrepriseIdPaginated(entrepriseId, pageable);

        // --- 7. Récupérer les statistiques globales (une seule fois) ---
        long totalProduitsUniques = produitRepository.countProduitsUniquesByEntrepriseId(entrepriseId);
        long totalBoutiques = produitRepository.countBoutiquesActivesByEntrepriseId(entrepriseId);

        // --- 8. Traiter les produits de la page courante ---
        Map<String, ProduitDTO> produitsUniques = new HashMap<>();

        for (Produit produit : produitsPage.getContent()) {
            if (Boolean.TRUE.equals(produit.getDeleted())) continue;

            Boutique boutique = produit.getBoutique();
            if (boutique != null && boutique.isActif()) {
                String codeGenerique = produit.getCodeGenerique();

                ProduitDTO produitDTO = produitsUniques.computeIfAbsent(codeGenerique, k -> {
                    ProduitDTO dto = convertToProduitDTO(produit);
                    dto.setBoutiques(new ArrayList<>());
                    dto.setQuantite(0);
                    return dto;
                });

                Map<String, Object> boutiqueInfo = new HashMap<>();
                boutiqueInfo.put("nom", boutique.getNomBoutique());
                boutiqueInfo.put("id", boutique.getId());
                boutiqueInfo.put("typeBoutique", boutique.getTypeBoutique());
                boutiqueInfo.put("quantite", produit.getQuantite());

                produitDTO.getBoutiques().add(boutiqueInfo);
                produitDTO.setQuantite(produitDTO.getQuantite() + produit.getQuantite());
            }
        }

        // --- 9. Créer la page de DTOs ---
        List<ProduitDTO> produitsDTOs = new ArrayList<>(produitsUniques.values());
        Page<ProduitDTO> dtoPage = new PageImpl<>(
                produitsDTOs,
                pageable,
                totalProduitsUniques
        );

        // --- 10. Retourner la réponse paginée ---
        return ProduitEntreprisePaginatedResponseDTO.fromPage(dtoPage, totalProduitsUniques, totalBoutiques);
    }

  

    public Map<String, Object> importProduitsFromExcel(
            InputStream inputStream,
            Long entrepriseId,
            List<Long> boutiqueIds,
            String tokenHeader, // Token complet avec "Bearer"
            HttpServletRequest request) {

        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            // 1. Vérification du token JWT
            if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Token JWT manquant ou mal formaté");
            }

            // Extraire le token sans "Bearer "
            String token = tokenHeader.substring(7);

            User user = authHelper.getAuthenticatedUserWithFallback(request);

            // 4. Vérification de l'entreprise
            Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                    .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

            // 5. Vérification des permissions
            RoleType role = user.getRole().getName();
            boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
            boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS)
                    || user.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK);

            if (!isAdminOrManager && !hasPermission) {
                throw new RuntimeException("Accès refusé : permissions insuffisantes");
            }

            // 6. Vérification d'appartenance à l'entreprise
            if (!user.getEntreprise().getId().equals(entrepriseId)) {
                throw new RuntimeException("Accès interdit : utilisateur ne fait pas partie de cette entreprise");
            }

            // 7. Récupération des boutiques sélectionnées
            List<Boutique> selectedBoutiques;
            if (boutiqueIds != null && !boutiqueIds.isEmpty()) {
                // Récupérer les boutiques par leurs IDs
                selectedBoutiques = boutiqueRepository.findAllById(boutiqueIds);

                // Vérifier que chaque boutique appartient à l'entreprise et est active
                for (Boutique boutique : selectedBoutiques) {
                    if (!boutique.getEntreprise().getId().equals(entrepriseId)) {
                        throw new RuntimeException("La boutique ID " + boutique.getId() + " n'appartient pas à l'entreprise ID " + entrepriseId);
                    }
                    if (!boutique.isActif()) {
                        throw new RuntimeException("La boutique ID " + boutique.getId() + " est désactivée");
                    }
                }
            } else {
                // Si aucune boutique n'est spécifiée, on prend toutes les boutiques actives de l'entreprise
                selectedBoutiques = boutiqueRepository.findByEntrepriseIdAndActifTrue(entrepriseId);
            }

            // Convertir en liste d'IDs
            List<Long> boutiqueIdsFinal = selectedBoutiques.stream()
                    .map(Boutique::getId)
                    .collect(Collectors.toList());

            // 8. Traitement du fichier Excel
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            bis.mark(Integer.MAX_VALUE);

            Workbook workbook;
            try {
                // Essayer de lire comme OOXML (.xlsx)
                workbook = WorkbookFactory.create(bis);
            } catch (NotOfficeXmlFileException | OfficeXmlFileException e) {
                // Réessayer comme OLE2 (.xls)
                bis.reset();
                workbook = new HSSFWorkbook(bis);
            } catch (Exception e) {
                throw new RuntimeException("Format de fichier non reconnu", e);
            }

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();
            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.FRENCH));

            Iterator<Row> rowIterator = sheet.iterator();

            // Sauter l'en-tête
            if (rowIterator.hasNext()) rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    // Vérifier si la ligne est vide
                    if (isRowEmpty(row)) {
                        continue;
                    }

                    ProduitRequest produitRequest = mapRowToProduitRequest(row, dataFormatter, decimalFormat);

                    // Créer le produit dans les boutiques sélectionnées
                    List<Integer> quantites = new ArrayList<>();
                    List<Integer> seuils = new ArrayList<>();
                    for (int i = 0; i < boutiqueIdsFinal.size(); i++) {
                        quantites.add(produitRequest.getQuantite() != null ? produitRequest.getQuantite() : 0);
                        seuils.add(produitRequest.getSeuilAlert() != null ? produitRequest.getSeuilAlert() : 0);
                    }

                    // Appel à createProduit avec le token et les IDs des boutiques
                    createProduit(
                            request,
                            boutiqueIdsFinal, // Utiliser les IDs des boutiques sélectionnées
                            quantites,
                            seuils,
                            produitRequest,
                            true,
                            null
                    );

                    successCount++;

                } catch (Exception e) {
                    errors.add("Ligne " + (row.getRowNum() + 1) + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            errors.add("Erreur système: " + e.getMessage());
            e.printStackTrace();
        }

        result.put("successCount", successCount);
        if (!errors.isEmpty()) {
            result.put("errors", errors);
        }
        return result;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private ProduitRequest mapRowToProduitRequest(Row row, DataFormatter dataFormatter, DecimalFormat decimalFormat) {
        ProduitRequest request = new ProduitRequest();

        try {
            // Colonne 0: Nom produit
            request.setNom(getStringValue(row, 0, dataFormatter));

            // Colonne 1: Description
            request.setDescription(getStringValue(row, 1, dataFormatter));

            // Colonne 2: Catégorie
            String categorieNom = getStringValue(row, 2, dataFormatter);
            if (categorieNom != null && !categorieNom.isEmpty()) {
                Optional<Categorie> categorieOpt = categorieRepository.findByNom(categorieNom);

                if (categorieOpt.isPresent()) {
                    request.setCategorieId(categorieOpt.get().getId());
                } else {
                    // Créer la catégorie si elle n'existe pas
                    Categorie newCategorie = new Categorie();
                    newCategorie.setNom(categorieNom);
                    Categorie savedCategorie = categorieRepository.save(newCategorie);
                    request.setCategorieId(savedCategorie.getId());
                }
            }

            // Colonne 3: Prix Vente
            request.setPrixVente(parseDouble(getStringValue(row, 3, dataFormatter), decimalFormat));

            // Colonne 4: Prix Achat
            request.setPrixAchat(parseDouble(getStringValue(row, 4, dataFormatter), decimalFormat));

            // Colonne 5: Quantité
            request.setQuantite(parseInt(getStringValue(row, 5, dataFormatter)));

            // Colonne 6: Unité
            String uniteNom = getStringValue(row, 6, dataFormatter);
            if (uniteNom != null && !uniteNom.isEmpty()) {
                Optional<Unite> uniteOpt = uniteRepository.findByNom(uniteNom);

                if (uniteOpt.isPresent()) {
                    request.setUniteId(uniteOpt.get().getId());
                } else {
                    // Créer l'unité si elle n'existe pas
                    Unite newUnite = new Unite();
                    newUnite.setNom(uniteNom);
                    Unite savedUnite = uniteRepository.save(newUnite);
                    request.setUniteId(savedUnite.getId());
                }
            }

            // Colonne 7: Code Barre
            request.setCodeBare(getStringValue(row, 7, dataFormatter));

            // Colonne 8: Type Produit
            String typeProduit = getStringValue(row, 8, dataFormatter);
            if (typeProduit != null && !typeProduit.isEmpty()) {
                try {
                    // Normaliser la casse
                    request.setTypeProduit(TypeProduit.valueOf(typeProduit.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Valeur par défaut si le type est invalide
                    request.setTypeProduit(TypeProduit.PHYSIQUE);
                }
            } else {
                // Valeur par défaut si non spécifié
                request.setTypeProduit(TypeProduit.PHYSIQUE);
            }

            // Colonne 10: Date Preemption (facultative)
            String datePreemptionStr = getStringValue(row, 9, dataFormatter);
            if (datePreemptionStr != null && !datePreemptionStr.isEmpty()) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
                    LocalDate datePreemption = LocalDate.parse(datePreemptionStr, formatter);
                    request.setDatePreemption(datePreemption);
                } catch (Exception e) {
                    throw new RuntimeException("Format de date invalide. Utilisez dd-MM-yyyy (ex: 31-12-2025)");
                }
            }

            // Colonne 9: Seuil Alert
            request.setSeuilAlert(parseInt(getStringValue(row, 10, dataFormatter)));


        } catch (Exception e) {
            throw new RuntimeException("Erreur ligne " + (row.getRowNum() + 1) + ": " + e.getMessage());
        }

        return request;
    }

    private String getStringValue(Row row, int cellIndex, DataFormatter dataFormatter) {
        if (row == null) return null;

        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;

        return dataFormatter.formatCellValue(cell).trim();
    }

    private Double parseDouble(String value, DecimalFormat decimalFormat) {
        if (value == null || value.isEmpty()) return null;

        try {
            // Nettoyage des formats numériques européens
            value = value.replace(" ", "").replace(".", "");
            return decimalFormat.parse(value).doubleValue();
        } catch (Exception e) {
            throw new RuntimeException("Valeur numérique invalide: " + value);
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.isEmpty()) return null;

        try {
            // Nettoyage des formats numériques européens
            value = value.replace(" ", "").replace(".", "");
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new RuntimeException("Valeur entière invalide: " + value);
        }
    }




}
