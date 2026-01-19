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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.xpertcash.DTOs.CompteurBoutiqueDTO;
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


@Service
public class ProduitService {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private BoutiqueRepository boutiqueRepository;


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
    @Transactional
    public List<ProduitDTO> createProduit(HttpServletRequest request, List<Long> boutiqueIds,
                                      List<Integer> quantites, List<Integer> seuilAlert, ProduitRequest produitRequest, boolean addToStock, String image) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour créer des produits.");
        }

        List<ProduitDTO> produitsCreated = new ArrayList<>();
        
        for (int i = 0; i < boutiqueIds.size(); i++) {
            Long boutiqueId = boutiqueIds.get(i);
            Integer quantite = quantites.get(i);
            Integer seuil = seuilAlert.get(i);
            
            Boutique boutique = boutiqueRepository.findById(boutiqueId)
                    .orElseThrow(() -> new RuntimeException("Boutique introuvable: " + boutiqueId));
            
            if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
                throw new RuntimeException("Accès interdit : cette boutique ne vous appartient pas");
            }
            
            Produit produit = createSingleProduit(produitRequest, boutique, quantite, seuil, addToStock, image);
            
            ProduitDTO dto = convertToProduitDTO(produit);
            produitsCreated.add(dto);
        }
        
        
        return produitsCreated;
    }
    
    // Méthode helper pour créer un seul produit
    private Produit createSingleProduit(ProduitRequest produitRequest, Boutique boutique, Integer quantite, Integer seuilAlert, boolean addToStock, String image) {
        String codeGenerique = generateUniqueCode(boutique.getEntreprise().getId());
        
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
        
        if (produitRequest.getCategorieId() != null) {
            Categorie categorie = categorieRepository.findById(produitRequest.getCategorieId())
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            produit.setCategorie(categorie);
        } else {
            Categorie sansCategory = getOrCreateSansCategory(boutique.getEntreprise());
            produit.setCategorie(sansCategory);
        }
        
        if (produitRequest.getUniteId() != null) {
            Unite unite = uniteRepository.findById(produitRequest.getUniteId())
                    .orElseThrow(() -> new RuntimeException("Unité non trouvée"));
            produit.setUniteDeMesure(unite);
        }
        
        if (image != null && !image.isEmpty()) {
            produit.setPhoto(image);
        }
        
        if (produitRequest.getTypeProduit() != null) {
            produit.setTypeProduit(produitRequest.getTypeProduit());
        }
        
        if (produitRequest.getDatePreemption() != null) {
            produit.setDatePreemption(produitRequest.getDatePreemption());
        }
        
        produit = produitRepository.save(produit);
        
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

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        Long entrepriseId = boutique.getEntreprise().getId();

        if (!user.getEntreprise().getId().equals(entrepriseId)) {
            throw new RuntimeException("Accès interdit : cette boutique ne vous appartient pas");
        }

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

    produit.setQuantite(nouvelleQuantiteProduit);
    produitRepository.save(produit);

    stock.setStockActuel(nouvelleQuantiteProduit);
    stock.setQuantiteAjoute(quantiteAjoute);
    stock.setStockApres(nouvelleQuantiteProduit);
    stock.setLastUpdated(LocalDateTime.now());



    stockRepository.save(stock);

    if (fournisseurId != null) {
        fournisseurEntity = fournisseurRepository.findByIdAndEntrepriseId(fournisseurId, entrepriseId)
            .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé avec l'ID : " + fournisseurId + " ou n'appartient pas à votre entreprise"));
    }

    StockProduitFournisseur spf = new StockProduitFournisseur();
    spf.setStock(stock);
    spf.setProduit(produit);
    spf.setQuantiteAjoutee(quantiteAjoute);

    if (fournisseurEntity != null) {
        spf.setFournisseur(fournisseurEntity);
    }

    stockProduitFournisseurRepository.save(spf);



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

        return enregistrerFacture("AJOUTER", produits, produitsQuantites, description, codeFournisseur, fournisseurEntity, user);
    }

    // Méthode pour ajuster la quantité du produit en stock (retirer des produits)
    public FactureDTO retirerStock(Long boutiqueId, Map<Long, Integer> produitsQuantites, String description, HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        if (!boutique.isActif()) {
            throw new RuntimeException("La boutique est désactivée, opération non autorisée.");
        }

        Long entrepriseId = boutique.getEntreprise().getId();
        if (!entrepriseId.equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        boolean hasPermissionGestion = user.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK);
        

        if (!isAdminOrManager && !hasPermission && !hasPermissionGestion) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour retirer du stock !");
        }



    List<Produit> produits = new ArrayList<>();

    for (Map.Entry<Long, Integer> entry : produitsQuantites.entrySet()) {
        Long produitId = entry.getKey();
        Integer quantiteRetirer = entry.getValue();

        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

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

        produit.setQuantite(nouvelleQuantiteProduit);
        produitRepository.save(produit);

        stock.setStockActuel(nouvelleQuantiteProduit);
        stock.setQuantiteRetirer(quantiteRetirer);
        stock.setStockApres(nouvelleQuantiteProduit);
        stock.setLastUpdated(LocalDateTime.now());
        stockRepository.save(stock);

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

    Facture facture = enregistrerFacture("Réduction", produits, produitsQuantites, description, null, null, user);
    return new FactureDTO(facture);
}




      // Génère un numéro unique de facture
   private String generateNumeroFacture(Long entrepriseId) {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        List<Facture> facturesAnnee = factureRepository.findByYearAndEntrepriseId(currentYear, entrepriseId);

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
            return 0;
        }
    }


    // Méthode pour enregistrer une facture
   public Facture enregistrerFacture(String type, List<Produit> produits, Map<Long, Integer> quantites,
                                  String description, String codeFournisseur, Fournisseur fournisseur, User user) {
    Facture facture = new Facture();
    Long entrepriseId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;
    if (entrepriseId == null && !produits.isEmpty() && produits.get(0).getBoutique() != null) {
        entrepriseId = produits.get(0).getBoutique().getEntreprise().getId();
    }
    if (entrepriseId == null) {
        throw new RuntimeException("Impossible de déterminer l'entreprise pour générer le numéro de facture.");
    }
    facture.setNumeroFacture(generateNumeroFacture(entrepriseId));
    facture.setType(type);
    facture.setDescription(description);
    facture.setDateFacture(LocalDateTime.now());
    facture.setUser(user);

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
        
        Double prixUnitaire = produit.getPrixVente();
        if (prixUnitaire == null) {
            prixUnitaire = 0.0;
        }
        factureProduit.setPrixUnitaire(prixUnitaire);
        factureProduit.setTotal(factureProduit.getQuantite() * factureProduit.getPrixUnitaire());
        factureProduits.add(factureProduit);
    }

    facture.setFactureProduits(factureProduits);

    if (codeFournisseur != null && !codeFournisseur.isEmpty()) {
        facture.setCodeFournisseur(codeFournisseur);
    }

    if ("Ajout".equalsIgnoreCase(type) || "Approvisionnement".equalsIgnoreCase(type)) {
        if (fournisseur == null) {
            throw new RuntimeException("Le fournisseur est requis pour une facture de type '" + type + "'");
        }

        Fournisseur fournisseurEntity = fournisseurRepository.findByIdAndEntrepriseId(
                fournisseur.getId(), entrepriseId)
            .orElseThrow(() -> new RuntimeException("Fournisseur introuvable ou n'appartient pas à votre entreprise"));
        facture.setFournisseur(fournisseurEntity);
    } else if (fournisseur != null) {
        Fournisseur fournisseurEntity = fournisseurRepository.findByIdAndEntrepriseId(
                fournisseur.getId(), entrepriseId)
            .orElseThrow(() -> new RuntimeException("Fournisseur introuvable ou n'appartient pas à votre entreprise"));
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

    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS)
                            || user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);

    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour consulter l'historique de stock.");
    }

    Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé avec l'ID : " + produitId));

    Boutique boutique = produit.getBoutique();
    if (boutique == null || boutique.getEntreprise() == null) {
        throw new RuntimeException("Le produit ou sa boutique ne sont pas liés à une entreprise.");
    }

    Long entrepriseProduitId = boutique.getEntreprise().getId();
    Long entrepriseUserId = user.getEntreprise().getId();

    if (!entrepriseProduitId.equals(entrepriseUserId)) {
        throw new RuntimeException("Accès interdit : ce produit n'appartient pas à votre entreprise !");
    }

    Stock stock = stockRepository.findByProduit(produit);
    if (stock == null) {
        throw new RuntimeException("Aucun stock associé à ce produit.");
    }

    List<StockHistory> stockHistories = stockHistoryRepository.findByStock(stock);
    if (stockHistories.isEmpty()) {
        throw new RuntimeException("Aucun historique de stock trouvé pour ce produit.");
    }

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


    public List<StockHistoryDTO> getAllStockHistory(HttpServletRequest request) {

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    if (user.getEntreprise() == null) {
        throw new RuntimeException("Utilisateur non rattaché à une entreprise.");
    }

    Long entrepriseId = user.getEntreprise().getId();

    //  Récupérer tous les historiques filtrés par entreprise
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

    RoleType role = admin.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = admin.getRole().hasPermission(PermissionType.GERER_PRODUITS);

    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour modifier un produit !");
    }

    Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

    Long produitEntrepriseId = produit.getBoutique().getEntreprise().getId();
    Long adminEntrepriseId = admin.getEntreprise().getId();
    if (!produitEntrepriseId.equals(adminEntrepriseId)) {
        throw new RuntimeException("Accès interdit : ce produit n'appartient pas à votre entreprise.");
    }

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
        if (produit.getPhoto() != null && !produit.getPhoto().isBlank()) {
            String oldPhotoPathStr = "src/main/resources/static" + produit.getPhoto();
            Path oldPhotoPath = Paths.get(oldPhotoPathStr);
            try {
                Files.deleteIfExists(oldPhotoPath);
                System.out.println(" Ancienne photo supprimée : " + oldPhotoPathStr);
            } catch (IOException e) {
                System.err.println(" Erreur lors de la suppression de l'ancienne photo : " + e.getMessage());
            }
        }

        String newPhotoPath = imageStorageService.saveImage(imageFile);
        produit.setPhoto(newPhotoPath);
    }



    if (produitRequest.getCategorieId() != null) {
        Categorie categorie = categorieRepository.findById(produitRequest.getCategorieId())
                .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
        produit.setCategorie(categorie);
    }

    if (produitRequest.getUniteId() != null) {
        Unite unite = uniteRepository.findById(produitRequest.getUniteId())
                .orElseThrow(() -> new RuntimeException("Unité de mesure non trouvée"));
        produit.setUniteDeMesure(unite);
    }

    produitRepository.save(produit);

    if (addToStock) {
        Stock stock = stockRepository.findByProduit(produit);

        if (stock == null) {
            Stock newStock = new Stock();
            newStock.setProduit(produit);
            newStock.setStockActuel(produit.getQuantite() != null ? produit.getQuantite() : 0);
            newStock.setBoutique(produit.getBoutique());
            newStock.setCreatedAt(LocalDateTime.now());
            newStock.setLastUpdated(LocalDateTime.now());

            if (produitRequest.getSeuilAlert() != null) {
                newStock.setSeuilAlert(produitRequest.getSeuilAlert());
            } else {
                newStock.setSeuilAlert(produit.getSeuilAlert());
            }

            stockRepository.save(newStock);
        } else {
            stock.setStockActuel(produit.getQuantite() != null ? produit.getQuantite() : 0);

            stock.setQuantiteAjoute(0);
            stock.setQuantiteRetirer(0);


            if (produitRequest.getSeuilAlert() != null) {
                stock.setSeuilAlert(produitRequest.getSeuilAlert());
            }

            stock.setLastUpdated(LocalDateTime.now());
            stockRepository.save(stock);
        }

        produit.setEnStock(true);
    } else {
        Stock stock = stockRepository.findByProduit(produit);

        if (stock != null) {
            List<StockHistory> historyRecords = stockHistoryRepository.findByStock(stock);
            if (!historyRecords.isEmpty()) {
                stockHistoryRepository.deleteAll(historyRecords);
            }

            stockRepository.delete(stock);
        }



        produit.setEnStock(false);
    }

    produitRepository.save(produit);

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
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Long produitEntrepriseId = produit.getBoutique().getEntreprise().getId();
        Long userEntrepriseId = user.getEntreprise().getId();
        if (!produitEntrepriseId.equals(userEntrepriseId)) {
            throw new RuntimeException("Action interdite : ce produit n'appartient pas à votre entreprise.");
        }

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Action non autorisée : permissions insuffisantes");
        }

        if (produit.getEnStock()) {
            throw new RuntimeException(" Impossible de supprimer le produit car il est encore en stock");
        }

        boolean produitUtilise = ligneFactureReelleRepository.existsByProduitIdAndEntrepriseId(produitId, userEntrepriseId);
        boolean produitUtiliseProforma = ligneFactureProformaRepository.existsByProduitIdAndEntrepriseId(produitId, userEntrepriseId);
        
        if (produitUtilise || produitUtiliseProforma) {
            throw new RuntimeException(" Impossible de supprimer le produit car il est lié à des factures");
        }
        
        
 
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
            System.out.println(" Stock supprimé et produit marqué comme 'hors stock'");
        } else {
            throw new RuntimeException("Aucun stock trouvé pour ce produit !");
        }
    }

    // Méthode pour restaurer un ou plusieurs produit depuis la corbeille
    @Transactional
    public void restaurerProduitsDansBoutique(Long boutiqueId, List<Long> produitIds, HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        User user = authHelper.getAuthenticatedUserWithFallback(request);

         RoleType role = user.getRole().getName();
      boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
       boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);


       if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Action non autorisée : permissions insuffisantes");
        }

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        for (Long produitId : produitIds) {
            Produit produit = produitRepository.findById(produitId)
                    .orElseThrow(() -> new RuntimeException("Produit non trouvé : ID " + produitId));

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

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        boolean isAdmin = user.getRole().getName() == RoleType.ADMIN;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAdmin && !hasPermission) {
            throw new RuntimeException("Action non autorisée : permissions insuffisantes");
        }

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Action interdite : cette boutique ne vous appartient pas");
        }

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
        
        
        
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        
        if (boutiqueId == null) {
            throw new RuntimeException("L'ID de la boutique ne peut pas être null");
        }
        
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));
        if (!boutique.isActif()) {
            throw new RuntimeException("Cette boutique est désactivée, ses produits ne sont pas accessibles !");
        }

        Long entrepriseId = boutique.getEntreprise().getId();
        if (!entrepriseId.equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique ne vous appartient pas");
        }

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS)
                            || user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS)
                            || user.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK)
                            || user.getRole().hasPermission(PermissionType.GERER_BOUTIQUE);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès interdit : vous n'avez pas les droits pour consulter les produits.");
        }

        boolean isVendeur = role == RoleType.VENDEUR;
        if (isVendeur) {
            Optional<UserBoutique> userBoutique = userBoutiqueRepository.findByUserIdAndBoutiqueId(user.getId(), boutiqueId);
            if (!userBoutique.isPresent()) {
                throw new RuntimeException("Vous n'êtes pas affecté à cette boutique, vous ne pouvez pas voir ses produits.");
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());

        Page<Produit> produitsPage = produitRepository.findProduitsByBoutiqueIdPaginated(boutiqueId, pageable);

        long totalProduitsActifs = produitRepository.countProduitsActifsByBoutiqueId(boutiqueId);
        long totalProduitsEnStock = produitRepository.countProduitsEnStockByBoutiqueId(boutiqueId);
        long totalProduitsHorsStock = produitRepository.countProduitsHorsStockByBoutiqueId(boutiqueId);

        List<ProduitDTO> produitsDTOs = produitsPage.getContent().stream()
                .map(this::convertToProduitDTO)
                .collect(Collectors.toList());

        Page<ProduitDTO> dtoPage = new PageImpl<>(
                produitsDTOs,
                pageable,
                totalProduitsActifs
        );

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


            produitDTO.setNomCategorie(produit.getCategorie() != null ? produit.getCategorie().getNom() : null);
            produitDTO.setNomUnite(produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getNom() : null);
            TypeProduit type = produit.getTypeProduit();
            produitDTO.setTypeProduit(type != null ? type.name() : null);
            produitDTO.setBoutiqueId(produit.getBoutique() != null ? produit.getBoutique().getId() : null);


            return produitDTO;
        }

    // Méthode pour lister les produits dans la corbeille
    @Transactional(readOnly = true)
    public List<ProduitDTO> getProduitsDansCorbeille(Long boutiqueId, HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

    if (!boutique.isActif()) {
        throw new RuntimeException("Cette boutique est désactivée !");
    }

    if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : cette boutique ne vous appartient pas");
    }

    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasProduitPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
    boolean hasVentePermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);

    if (!isAdminOrManager && !hasProduitPermission && !hasVentePermission) {
        throw new RuntimeException("Accès interdit : vous n'avez pas les droits pour consulter la corbeille.");
    }

    List<Produit> produitsSupprimes = produitRepository.findByBoutiqueIdAndDeletedTrue(boutiqueId);

    return produitsSupprimes.stream()
            .map(this::convertToProduitDTO)
            .collect(Collectors.toList());
}

    //Methode Total des Produit:
    public Map<String, Integer> getTotalQuantitesParStock(Long boutiqueId, HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        if (!boutique.getEntreprise().getId().equals(entreprise.getId())) {
            throw new RuntimeException("Accès interdit : cette boutique ne vous appartient pas");
        }

        List<Produit> produitsEnStock = produitRepository.findByBoutiqueIdAndEnStockTrue(boutiqueId);
        List<Produit> produitsNonEnStock = produitRepository.findByBoutiqueIdAndEnStockFalse(boutiqueId);

        int totalEnStock = produitsEnStock.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .mapToInt(Produit::getQuantite)
                .sum();
        int totalNonEnStock = produitsNonEnStock.stream()
                .filter(p -> !Boolean.TRUE.equals(p.getDeleted()))
                .mapToInt(Produit::getQuantite)
                .sum();

        Map<String, Integer> totals = new HashMap<>();
        totals.put("totalEnStock", totalEnStock);
        totals.put("totalNonEnStock", totalNonEnStock);

        return totals;
    }

   public ProduitDTO getProduitById(Long id, HttpServletRequest request) {
    User admin = authHelper.getAuthenticatedUserWithFallback(request);

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

    if (produit.getBoutique() == null || produit.getBoutique().getEntreprise() == null) {
        throw new RuntimeException("Produit ou boutique non rattaché à une entreprise !");
    }

    if (!produit.getBoutique().getEntreprise().getId().equals(admin.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : ce produit n'appartient pas à votre entreprise !");
    }

    ProduitDTO dto = mapToProduitDTO(produit);

    List<Map<String, Object>> boutiquesInfo = new ArrayList<>();

    List<Produit> produitsMemeCode = produitRepository.findByCodeGeneriqueAndEntrepriseId(
            produit.getCodeGenerique(), 
            admin.getEntreprise().getId());

    int totalQuantite = 0;

    for (Produit p : produitsMemeCode) {
        if (p.getBoutique() != null && p.getBoutique().isActif()) {

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
        
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
        
        if (entrepriseId == null) {
            throw new RuntimeException("L'ID de l'entreprise ne peut pas être null");
        }
        
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

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermissionGestionProduits = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        boolean hasPermissionGestionFacturation = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);
        boolean hasPermissionApprovisionnerStock = user.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK);

        if (!isAdminOrManager && !hasPermissionGestionProduits && !hasPermissionGestionFacturation && !hasPermissionApprovisionnerStock) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires pour consulter les produits.");
        }

        if (!entreprise.getId().equals(entrepriseId)) {
            throw new RuntimeException("Accès refusé : vous ne pouvez pas accéder aux produits d'une autre entreprise. Votre entreprise: " + entreprise.getId() + ", Demandée: " + entrepriseId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("codeGenerique").ascending().and(Sort.by("nom").ascending()));

        Page<Produit> produitsPage = produitRepository.findProduitsByEntrepriseIdPaginated(entrepriseId, pageable);

        long totalProduitsUniques = produitRepository.countProduitsUniquesByEntrepriseId(entrepriseId);
        long totalBoutiques = produitRepository.countBoutiquesActivesByEntrepriseId(entrepriseId);

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

        List<ProduitDTO> produitsDTOs = new ArrayList<>(produitsUniques.values());
        Page<ProduitDTO> dtoPage = new PageImpl<>(
                produitsDTOs,
                pageable,
                totalProduitsUniques
        );

        return ProduitEntreprisePaginatedResponseDTO.fromPage(dtoPage, totalProduitsUniques, totalBoutiques);
    }

  

    public Map<String, Object> importProduitsFromExcel(
            InputStream inputStream,
            Long entrepriseId,
            List<Long> boutiqueIds,
            String tokenHeader,
            HttpServletRequest request) {

        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            if (tokenHeader == null || !tokenHeader.startsWith("Bearer ")) {
                throw new RuntimeException("Token JWT manquant ou mal formaté");
            }

            String token = tokenHeader.substring(7);

            User user = authHelper.getAuthenticatedUserWithFallback(request);

            Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                    .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));

            RoleType role = user.getRole().getName();
            boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
            boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS)
                    || user.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK);

            if (!isAdminOrManager && !hasPermission) {
                throw new RuntimeException("Accès refusé : permissions insuffisantes");
            }

            if (!user.getEntreprise().getId().equals(entrepriseId)) {
                throw new RuntimeException("Accès interdit : utilisateur ne fait pas partie de cette entreprise");
            }

            List<Boutique> selectedBoutiques;
            if (boutiqueIds != null && !boutiqueIds.isEmpty()) {
                selectedBoutiques = boutiqueRepository.findAllById(boutiqueIds);

                for (Boutique boutique : selectedBoutiques) {
                    if (!boutique.getEntreprise().getId().equals(entrepriseId)) {
                        throw new RuntimeException("La boutique ID " + boutique.getId() + " n'appartient pas à l'entreprise ID " + entrepriseId);
                    }
                    if (!boutique.isActif()) {
                        throw new RuntimeException("La boutique ID " + boutique.getId() + " est désactivée");
                    }
                }
            } else {
                selectedBoutiques = boutiqueRepository.findByEntrepriseIdAndActifTrue(entrepriseId);
            }

            List<Long> boutiqueIdsFinal = selectedBoutiques.stream()
                    .map(Boutique::getId)
                    .collect(Collectors.toList());

            BufferedInputStream bis = new BufferedInputStream(inputStream);
            bis.mark(Integer.MAX_VALUE);

            Workbook workbook;
            try {
                workbook = WorkbookFactory.create(bis);
            } catch (NotOfficeXmlFileException | OfficeXmlFileException e) {
                bis.reset();
                workbook = new HSSFWorkbook(bis);
            } catch (Exception e) {
                throw new RuntimeException("Format de fichier non reconnu", e);
            }

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();
            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(Locale.FRENCH));

            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                try {
                    if (isRowEmpty(row)) {
                        continue;
                    }

                    ProduitRequest produitRequest = mapRowToProduitRequest(row, dataFormatter, decimalFormat, user.getEntreprise());

                    List<Integer> quantites = new ArrayList<>();
                    List<Integer> seuils = new ArrayList<>();
                    for (int i = 0; i < boutiqueIdsFinal.size(); i++) {
                        quantites.add(produitRequest.getQuantite() != null ? produitRequest.getQuantite() : 0);
                        seuils.add(produitRequest.getSeuilAlert() != null ? produitRequest.getSeuilAlert() : 0);
                    }

                    createProduit(
                            request,
                            boutiqueIdsFinal,
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

    private ProduitRequest mapRowToProduitRequest(Row row, DataFormatter dataFormatter, DecimalFormat decimalFormat, Entreprise entreprise) {
        ProduitRequest request = new ProduitRequest();

        try {
            request.setNom(getStringValue(row, 0, dataFormatter));

            request.setDescription(getStringValue(row, 1, dataFormatter));

            String categorieNom = getStringValue(row, 2, dataFormatter);
            if (categorieNom != null && !categorieNom.isEmpty()) {
                Categorie categorie = categorieRepository.findByNomAndEntrepriseId(categorieNom, entreprise.getId());

                if (categorie != null) {
                    request.setCategorieId(categorie.getId());
                } else {
                    Categorie newCategorie = new Categorie();
                    newCategorie.setNom(categorieNom);
                    newCategorie.setEntreprise(entreprise);
                    newCategorie.setCreatedAt(LocalDateTime.now());
                    newCategorie.setProduitCount(0);
                    newCategorie.setOrigineCreation("PRODUIT");
                    Categorie savedCategorie = categorieRepository.save(newCategorie);
                    request.setCategorieId(savedCategorie.getId());
                }
            }

            request.setPrixVente(parseDouble(getStringValue(row, 3, dataFormatter), decimalFormat));

            request.setPrixAchat(parseDouble(getStringValue(row, 4, dataFormatter), decimalFormat));

            request.setQuantite(parseInt(getStringValue(row, 5, dataFormatter)));

            String uniteNom = getStringValue(row, 6, dataFormatter);
            if (uniteNom != null && !uniteNom.isEmpty()) {
                Optional<Unite> uniteOpt = uniteRepository.findByNomAndEntrepriseId(uniteNom, entreprise.getId());

                if (uniteOpt.isPresent()) {
                    request.setUniteId(uniteOpt.get().getId());
                } else {
                    Unite newUnite = new Unite();
                    newUnite.setNom(uniteNom);
                    newUnite.setEntreprise(entreprise);
                    Unite savedUnite = uniteRepository.save(newUnite);
                    request.setUniteId(savedUnite.getId());
                }
            }

            request.setCodeBare(getStringValue(row, 7, dataFormatter));

            String typeProduit = getStringValue(row, 8, dataFormatter);
            if (typeProduit != null && !typeProduit.isEmpty()) {
                try {
                    request.setTypeProduit(TypeProduit.valueOf(typeProduit.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    request.setTypeProduit(TypeProduit.PHYSIQUE);
                }
            } else {
                request.setTypeProduit(TypeProduit.PHYSIQUE);
            }

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
            value = value.replace(" ", "").replace(".", "");
            return decimalFormat.parse(value).doubleValue();
        } catch (Exception e) {
            throw new RuntimeException("Valeur numérique invalide: " + value);
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.isEmpty()) return null;

        try {
            value = value.replace(" ", "").replace(".", "");
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new RuntimeException("Valeur entière invalide: " + value);
        }
    }

    /**
     * Récupère ou crée la catégorie "Sans Category" si elle n'existe pas
     */
    private Categorie getOrCreateSansCategory(Entreprise entreprise) {
        Categorie sansCategory = categorieRepository.findByNomAndEntrepriseId("Sans Category", entreprise.getId());
        
        if (sansCategory != null) {
            return sansCategory;
        }
        
        sansCategory = new Categorie();
        sansCategory.setNom("Sans Category");
        sansCategory.setCreatedAt(LocalDateTime.now());
        sansCategory.setProduitCount(0);
        sansCategory.setEntreprise(entreprise);
        sansCategory.setOrigineCreation("PRODUIT");
        
        return categorieRepository.save(sansCategory);
    }


    /**
     * Génère un code unique de 6 caractères pour une entreprise
     * Format: P + ID entreprise + 3 chiffres (timestamp + compteur si nécessaire)
     */
    private String generateUniqueCode(Long entrepriseId) {
        String baseCode = "P" + entrepriseId;
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(10);
        String codeGenerique = baseCode + timestamp;
        int attempts = 0;
        while (codeExistsInEntreprise(codeGenerique, entrepriseId) && attempts < 100) {
            attempts++;
            String counter = String.format("%05d", attempts);
            codeGenerique = baseCode + counter;
        }
        
        if (attempts >= 100) {
            codeGenerique = baseCode + String.valueOf(System.nanoTime()).substring(10);
        }
        
        return codeGenerique;
    }
    
    /**
     * Vérifie si un code générique existe déjà dans une entreprise
     */
    private boolean codeExistsInEntreprise(String codeGenerique, Long entrepriseId) {
        List<Produit> produits = produitRepository.findByCodeGeneriqueAndEntrepriseId(codeGenerique, entrepriseId);
        return !produits.isEmpty();
    }

    /**
     * Récupère les compteurs de produits par boutique pour l'entreprise de l'utilisateur connecté
     */
    public List<CompteurBoutiqueDTO> getCompteursBoutiques(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        if (user.getEntreprise() == null) {
            throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
        }

        Long entrepriseId = user.getEntreprise().getId();

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS)
                       || user.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à cette information.");
        }

        List<Boutique> boutiques = boutiqueRepository.findByEntrepriseId(entrepriseId);
        
        List<CompteurBoutiqueDTO> compteurs = new ArrayList<>();

        for (Boutique boutique : boutiques) {
            List<Produit> produitsBoutique = produitRepository.findByBoutiqueIdAndNotDeleted(boutique.getId());
            
            long totalProduits = produitsBoutique.size();
            long totalEnStock = 0;

            for (Produit produit : produitsBoutique) {
                Stock stock = stockRepository.findByProduit(produit);
                
                if (stock != null && stock.getStockActuel() != null && stock.getStockActuel() > 0) {
                    totalEnStock++;
                } else if (produit.getEnStock() != null && produit.getEnStock()) {
                    totalEnStock++;
                }
            }

            CompteurBoutiqueDTO compteur = new CompteurBoutiqueDTO();
            compteur.setBoutiqueId(boutique.getId());
            compteur.setNomBoutique(boutique.getNomBoutique());
            compteur.setTotalProduits(totalProduits);
            compteur.setTotalEnStock(totalEnStock);
            
            compteurs.add(compteur);
        }

        return compteurs;
    }

}
