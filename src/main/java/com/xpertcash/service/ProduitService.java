package com.xpertcash.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.xpertcash.DTOs.FactureDTO;
import com.xpertcash.DTOs.ProduitDTO;
import com.xpertcash.DTOs.ProduitRequest;
import com.xpertcash.DTOs.StockHistoryDTO;
import com.xpertcash.configuration.JwtUtil;
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
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.TypeProduit;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.CategorieRepository;
import com.xpertcash.repository.FactureRepository;
import com.xpertcash.repository.FournisseurRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockHistoryRepository;
import com.xpertcash.repository.StockProduitFournisseurRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.UniteRepository;
import com.xpertcash.repository.UsersRepository;
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
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private JwtUtil jwtUtil; 

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


    // Ajouter un produit à la liste sans le stock
    public List<ProduitDTO> createProduit(HttpServletRequest request, List<Long> boutiqueIds, 
                                      List<Integer> quantites, List<Integer> seuilAlert, ProduitRequest produitRequest, boolean addToStock, String image) {
    try {
        // Vérification de la validité du token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        String jwtToken = token.substring(7);
        Long adminId = jwtUtil.extractUserId(jwtToken);
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        //autorisation et permission
        RoleType role = admin.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = admin.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits pour ajouter un produit !");
        }


        List<ProduitDTO> produitsAjoutes = new ArrayList<>();

        // Vérification que le nombre de boutiques et quantités est le même
        if (boutiqueIds.size() != quantites.size()) {
            throw new RuntimeException("Le nombre de boutiques ne correspond pas au nombre de quantités !");
        }

        // Récupérer la première boutique pour identifier l'entreprise
        Boutique premiereBoutique = boutiqueRepository.findById(boutiqueIds.get(0))
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));
        Long entrepriseId = premiereBoutique.getEntreprise().getId();

        // Vérifier si le produit existe déjà dans une boutique de la même entreprise
        Produit produitExistant = produitRepository.findByNomAndEntrepriseId(produitRequest.getNom(), entrepriseId);

        String codeGenerique;
        if (produitExistant != null) {
            // Réutiliser le codeGenerique existant
            codeGenerique = produitExistant.getCodeGenerique();
        } else {
            // Générer un nouveau codeGenerique unique
            codeGenerique = generateProductCode();
        }

        // Pour chaque boutique et sa quantité spécifique
        for (int i = 0; i < boutiqueIds.size(); i++) {
            Long boutiqueId = boutiqueIds.get(i);
            Integer quantite = quantites.get(i); // Quantité spécifique à cette boutique
            Integer seuil = seuilAlert.get(i);

            Boutique boutique = boutiqueRepository.findById(boutiqueId)
                    .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

            // Vérification si le produit est déjà enregistré dans cette boutique
            if (produitRepository.findByNomAndBoutiqueId(produitRequest.getNom(), boutiqueId) != null) {
                throw new RuntimeException("Un produit avec le même nom existe déjà dans la boutique ID: " + boutiqueId);
            }
            if (produitRequest.getCodeBare() != null && !produitRequest.getCodeBare().trim().isEmpty() &&
                    produitRepository.findByCodeBareAndBoutiqueId(produitRequest.getCodeBare().trim(), boutiqueId) != null) {
                throw new RuntimeException("Un produit avec le même code-barre existe déjà dans la boutique ID: " + boutiqueId);
            }

            // Récupérer la catégorie et unité
            Categorie categorie = (produitRequest.getCategorieId() != null) ?
                    categorieRepository.findById(produitRequest.getCategorieId())
                            .orElseThrow(() -> new RuntimeException("Catégorie non trouvée")) : null;

            Unite unite = (produitRequest.getUniteId() != null) ?
                    uniteRepository.findById(produitRequest.getUniteId())
                            .orElseThrow(() -> new RuntimeException("Unité de mesure non trouvée")) : null;

            Produit produit = new Produit();
            produit.setNom(produitRequest.getNom());
            produit.setDescription(produitRequest.getDescription());
            produit.setPrixVente(produitRequest.getPrixVente());
            produit.setPrixAchat(produitRequest.getPrixAchat());
            produit.setQuantite(quantite != null ? quantite : 0); // Utiliser la quantité pour chaque boutique
            produit.setSeuilAlert(seuil != null ? seuil : 0);
            produit.setTypeProduit(produitRequest.getTypeProduit());
            produit.setCategorie(categorie);
            produit.setUniteDeMesure(unite);
            produit.setCodeGenerique(codeGenerique);
            produit.setCodeBare(produitRequest.getCodeBare());
            produit.setPhoto(image);
            produit.setCreatedAt(LocalDateTime.now());
            produit.setLastUpdated(LocalDateTime.now());
            produit.setBoutique(boutique);

            Produit savedProduit = produitRepository.save(produit);

            // Ajouter au stock si demandé
            if (produitRequest.getTypeProduit() == TypeProduit.PHYSIQUE && addToStock) {
                Stock stock = new Stock();
                stock.setStockActuel(quantite != null ? quantite : 0);
                stock.setStockApres(stock.getStockActuel());
                stock.setQuantiteAjoute(0);
                stock.setBoutique(boutique);
                stock.setProduit(savedProduit);
                stock.setCreatedAt(LocalDateTime.now());
                stock.setLastUpdated(LocalDateTime.now());
                stock.setSeuilAlert(seuil != null ? seuil : 0);
                stockRepository.save(stock);
                savedProduit.setEnStock(true);
                produitRepository.save(savedProduit);
            }

            produitsAjoutes.add(mapToDTO(savedProduit));
        }

        return produitsAjoutes;

    } catch (RuntimeException e) {
        System.err.println("Erreur lors de la création du produit : " + e.getMessage());
        throw new RuntimeException(e.getMessage(), e);
    }
}




    private ProduitDTO mapToDTO(Produit produit) {
        ProduitDTO produitDTO = new ProduitDTO();
        produitDTO.setId(produit.getId());
        produitDTO.setNom(produit.getNom());
        produitDTO.setPrixVente(produit.getPrixVente());
        produitDTO.setPrixAchat(produit.getPrixAchat());
        produitDTO.setQuantite(produit.getQuantite());
        produitDTO.setSeuilAlert(produit.getSeuilAlert());
        produitDTO.setCodeBare(produit.getCodeBare());
        produitDTO.setCodeGenerique(produit.getCodeGenerique());
        produitDTO.setPhoto(produit.getPhoto());
        produitDTO.setEnStock(produit.getEnStock());
        produitDTO.setCreatedAt(produit.getCreatedAt());
        produitDTO.setLastUpdated(produit.getLastUpdated());

    
        // Assigner les IDs des entités liées (pas directement les objets)
        if (produit.getCategorie() != null) {
            produitDTO.setCategorieId(produit.getCategorie().getId());
        }
        if (produit.getUniteDeMesure() != null) {
            produitDTO.setUniteId(produit.getUniteDeMesure().getId());
        }

       TypeProduit type = produit.getTypeProduit();
        produitDTO.setTypeProduit(type != null ? type.name() : null);
        

    
        return produitDTO;
    }
    
    private String generateProductCode() {
        String code;
        do {
            String randomCode = String.format("%05d", (int)(Math.random() * 100000));
            code = "P-" + randomCode;
        } while (produitRepository.existsByCodeGenerique(code));
    
        return code;
    }
     
    

    //Methode pour ajuster la quantiter du produit en stock
    public Facture ajouterStock(Long boutiqueId, Map<Long, Integer> produitsQuantites, String description, String codeFournisseur, Long fournisseurId, HttpServletRequest request) {

        // Récupérer le token JWT depuis le header "Authorization"
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
    
        String jwtToken = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwtToken);
    
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        //Verification et Permission
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits pour ajouter du stock !");
        }
    
        List<Produit> produits = new ArrayList<>();
        Fournisseur fournisseurEntity = null;

       for (Map.Entry<Long, Integer> entry : produitsQuantites.entrySet()) {
    Long produitId = entry.getKey();
    Integer quantiteAjoute = entry.getValue();

    Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

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
    // Récupérer le token JWT depuis le header "Authorization"
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    String jwtToken = token.substring(7);
    Long userId = jwtUtil.extractUserId(jwtToken);

    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    // Vérification des permissions
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;  
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour retirer du stock !");
    }


    List<Produit> produits = new ArrayList<>();

    for (Map.Entry<Long, Integer> entry : produitsQuantites.entrySet()) {
        Long produitId = entry.getKey();
        Integer quantiteRetirer = entry.getValue();

        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

        Stock stock = stockRepository.findByProduit(produit);
        if (stock == null) {
            throw new RuntimeException("Stock introuvable pour ce produit");
        }

        int stockAvant = stock.getStockActuel();
        if (quantiteRetirer > stockAvant) {
            throw new RuntimeException("Impossible de retirer plus que la quantité disponible en stock");
        }

        int nouvelleQuantiteProduit = produit.getQuantite() - quantiteRetirer;

        // Mettre à jour le produit et le stock
        produit.setQuantite(nouvelleQuantiteProduit);
        produitRepository.save(produit);

        stock.setStockActuel(nouvelleQuantiteProduit);
        stock.setQuantiteRetirer(quantiteRetirer);
        stock.setStockApres(nouvelleQuantiteProduit);
        stock.setLastUpdated(LocalDateTime.now());
        stockRepository.save(stock);

        // Enregistrer l'historique du stock
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

    Facture facture = enregistrerFacture("Réduction", produits, produitsQuantites, description, null,null, user);


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
        public List<StockHistoryDTO> getStockHistory(Long produitId) {
        // Vérifier si le produit existe
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new NoSuchElementException("Produit non trouvé avec l'ID : " + produitId));

        // Vérifier si un stock est associé au produit
        Stock stock = stockRepository.findByProduit(produit);
        if (stock == null) {
            throw new RuntimeException("Stock introuvable pour ce produit");
        }

        // Récupérer l'historique des stocks
        List<StockHistory> stockHistories = stockHistoryRepository.findByStock(stock);

        // Vérifier si l'historique est vide
        if (stockHistories.isEmpty()) {
            throw new RuntimeException("Aucun historique de stock trouvé pour ce produit");
        }

        // Mapper en DTO
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

                    User user = stockHistory.getUser();
                    if (user != null) {
                        dto.setNomComplet(user.getNomComplet());
                        dto.setPhone(user.getPhone());
                        if (user.getRole() != null) {
                            dto.setRole(user.getRole().getName());
                        }
                    }
                    
                    
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }


    // Récupérer tous les mouvements de stock
    public List<StockHistoryDTO> getAllStockHistory() {
        List<StockHistory> stockHistories = stockHistoryRepository.findAll();
    
        // Convertir en DTOs (si nécessaire)
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

            User user = stockHistory.getUser();
            if (user != null) {
                dto.setNomComplet(user.getNomComplet());
                dto.setPhone(user.getPhone());
            
                if (user.getRole() != null) {
                    dto.setRole(user.getRole().getName());
                }
            }
            
            return dto;
        })
                .collect(Collectors.toList());
    }
    
    
   //Lister Stock
   public List<Stock> getAllStocks() {
    return stockRepository.findAll();
}
 
    
   // Update Produit
    public ProduitDTO updateProduct(Long produitId, ProduitRequest produitRequest, MultipartFile imageFile, boolean addToStock, HttpServletRequest request)
 {
    // Vérification de l'autorisation de l'admin
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    token = token.replace("Bearer ", "");
    Long adminId = null;
    try {
        adminId = jwtUtil.extractUserId(token);
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'admin depuis le token", e);
    }

    User admin = usersRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

    // Autorisation et permission
    RoleType role = admin.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = admin.getRole().hasPermission(PermissionType.GERER_PRODUITS);
    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour modifier un produit !");
    }

    Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

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
        Long userId = jwtUtil.extractUserId(token);
        
        // 3. Vérification de l'utilisateur
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 4. Vérification des permissions
        RoleType role = user.getRole().getName();
        boolean isAuthorized = (role == RoleType.ADMIN || role == RoleType.MANAGER) 
                            && user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        
        if (!isAuthorized) {
            throw new RuntimeException("Action non autorisée : permissions insuffisantes");
        }

        // 5. Validation métier
        if (produit.getEnStock()) {
            throw new RuntimeException("Impossible de supprimer : produit toujours en stock");
        }

        // 6. Marquage comme supprimé
        produit.setDeleted(true);
        produit.setDeletedAt(LocalDateTime.now());
        produit.setDeletedBy(userId);
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
        Long userId = jwtUtil.extractUserId(token);

        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        RoleType role = user.getRole().getName();
        boolean isAuthorized = (role == RoleType.ADMIN || role == RoleType.MANAGER) 
                            && user.getRole().hasPermission(PermissionType.GERER_PRODUITS);

        if (!isAuthorized) {
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

    // Vérification token & user & permissions
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }
    String token = authHeader.substring(7);
    Long userId = jwtUtil.extractUserId(token);

    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    RoleType role = user.getRole().getName();
    boolean isAuthorized = (role == RoleType.ADMIN || role == RoleType.MANAGER) 
                        && user.getRole().hasPermission(PermissionType.GERER_PRODUITS);

    if (!isAuthorized) {
        throw new RuntimeException("Action non autorisée : permissions insuffisantes");
    }

    // Vérifier que la boutique existe
    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

    // Récupérer tous les produits supprimés dans la boutique
    List<Produit> produitsSupprimes = produitRepository.findByBoutiqueIdAndDeletedTrue(boutiqueId);

    // Suppression définitive
    produitRepository.deleteAll(produitsSupprimes);
}

    
    // Méthode programmée pour vider la corbeille automatiquement
   
    // Lister Produit par boutique (excluant les produits dans la corbeille)
    public List<ProduitDTO> getProduitsParStock(Long boutiqueId) {
        // Vérifier si la boutique existe et est active
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

        if (!boutique.isActif()) {
            throw new RuntimeException("Cette boutique est désactivée, ses produits ne sont pas accessibles !");
        }

        // Récupérer les produits non supprimés (deleted = false ou null) et enStock = false
        List<Produit> produitsEnStockFalse = produitRepository.findByBoutiqueIdAndEnStockFalseAndDeletedFalseOrDeletedIsNull(boutiqueId);

        // Récupérer les produits non supprimés (deleted = false ou null) et enStock = true
        List<Produit> produitsEnStockTrue = produitRepository.findByBoutiqueIdAndEnStockTrueAndDeletedFalseOrDeletedIsNull(boutiqueId);

        // Mapper les entités Produit en ProduitDTO
        List<ProduitDTO> produitsDTO = new ArrayList<>();

        // Ajouter les produits enStock = false au DTO
        for (Produit produit : produitsEnStockFalse) {
            produitsDTO.add(convertToProduitDTO(produit));
        }

        // Ajouter les produits enStock = true au DTO
        for (Produit produit : produitsEnStockTrue) {
            produitsDTO.add(convertToProduitDTO(produit));
        }

        return produitsDTO;
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
    Long userId = jwtUtil.extractUserId(token);

    // 2. Vérification de l'utilisateur
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    // 3. Vérification des permissions
    RoleType role = user.getRole().getName();
    boolean isAuthorized = (role == RoleType.ADMIN || role == RoleType.MANAGER) 
                        && user.getRole().hasPermission(PermissionType.GERER_PRODUITS);

    if (!isAuthorized) {
        throw new RuntimeException("Action non autorisée : permissions insuffisantes");
    }

    // 4. Vérification de la boutique
    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));

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
    public ProduitDTO getProduitById(Long id) {
        Produit produit = produitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produit avec ID " + id + " non trouvé"));
    
        ProduitDTO dto = mapToProduitDTO(produit);
    
        // Liste pour stocker les infos des boutiques
        List<Map<String, Object>> boutiquesInfo = new ArrayList<>();
        
        // Récupérer tous les produits ayant le même codeGenerique
        List<Produit> produitsMemeCode = produitRepository.findByCodeGenerique(produit.getCodeGenerique());
    
        // Variable pour accumuler la quantité totale
        int totalQuantite = 0;
    
        // Boucle pour traiter chaque produit avec le même codeGenerique
        for (Produit p : produitsMemeCode) {
            if (p.getBoutique() != null && p.getBoutique().isActif()) {
                Map<String, Object> boutiqueData = new HashMap<>();
                boutiqueData.put("id", p.getBoutique().getId());
                boutiqueData.put("nom", p.getBoutique().getNomBoutique());
                boutiqueData.put("quantite", p.getQuantite()); // Quantité pour cette boutique
    
                // Ajouter cette boutique à la liste
                boutiquesInfo.add(boutiqueData);
    
                // Ajouter la quantité de ce produit à la quantité totale
                totalQuantite += p.getQuantite();
            }
        }
    
        // Définir la quantité totale dans le DTO
        dto.setQuantite(totalQuantite);  // Mettre à jour la quantité totale
        dto.setBoutiques(boutiquesInfo); // Ajouter la liste des boutiques
    
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
        return dto;
    }
    
        // Méthode pour récupérer tous les produits de toutes les boutiques d'une entreprise
        public List<ProduitDTO> getProduitsParEntreprise(Long entrepriseId) {
            // Récupérer tous les produits de l'entreprise
            List<Produit> produits = produitRepository.findByEntrepriseId(entrepriseId);
        
            // Regrouper les produits par codeGenerique
            Map<String, ProduitDTO> produitsUniques = new HashMap<>();
        
            for (Produit produit : produits) {
                if (produit.getBoutique() != null && produit.getBoutique().isActif()) {
                    String codeGenerique = produit.getCodeGenerique();
        
                    // Vérifier si ce produit unique existe déjà dans la map
                    if (!produitsUniques.containsKey(codeGenerique)) {
                        ProduitDTO produitDTO = convertToProduitDTO(produit);
                        produitDTO.setBoutiques(new ArrayList<>()); // Initialiser la liste des boutiques
                        produitDTO.setQuantite(0); // Initialiser la quantité totale à 0
                        produitsUniques.put(codeGenerique, produitDTO);
                    }
        
                    // Ajouter la boutique et sa quantité
                    Boutique boutique = produit.getBoutique();
                    Map<String, Object> boutiqueInfo = new HashMap<>();
                    boutiqueInfo.put("nom", boutique.getNomBoutique());
                    boutiqueInfo.put("id", boutique.getId());
                    boutiqueInfo.put("quantite", produit.getQuantite());
        
                    produitsUniques.get(codeGenerique).getBoutiques().add(boutiqueInfo);
        
                    // Additionner la quantité totale
                    produitsUniques.get(codeGenerique).setQuantite(
                        produitsUniques.get(codeGenerique).getQuantite() + produit.getQuantite()
                    );
                }
            }
        
            return new ArrayList<>(produitsUniques.values());
        }
        

    

}
