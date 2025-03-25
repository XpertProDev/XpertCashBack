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
import org.springframework.stereotype.Service;
import com.xpertcash.DTOs.FactureDTO;
import com.xpertcash.DTOs.ProduitDTO;
import com.xpertcash.DTOs.ProduitRequest;
import com.xpertcash.DTOs.StockHistoryDTO;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Categorie;
import com.xpertcash.entity.Facture;
import com.xpertcash.entity.FactureProduit;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.StockHistory;
import com.xpertcash.entity.Unite;
import com.xpertcash.entity.User;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.CategorieRepository;
import com.xpertcash.repository.FactureRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockHistoryRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.UniteRepository;
import com.xpertcash.repository.UsersRepository;
import jakarta.servlet.http.HttpServletRequest;


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


    // Ajouter un produit à la liste sans le stock
    public ProduitDTO createProduit(HttpServletRequest request, Long boutiqueId, ProduitRequest produitRequest, boolean addToStock) {
        try {
            // Récupérer le token JWT depuis le header "Authorization"
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                throw new RuntimeException("Token JWT manquant ou mal formaté");
            }
    
            // Extraire le token sans le "Bearer " (les 7 premiers caractères)
            String jwtToken = token.substring(7);
            Long adminId = jwtUtil.extractUserId(jwtToken);  // Extraire l'ID de l'admin à partir du token
    
            // Récupérer l'utilisateur (admin) à partir de l'ID extrait
            User admin = usersRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin non trouvé"));
    
            // Vérifier que l'utilisateur a bien le rôle ADMIN
            if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
                throw new RuntimeException("Seul un ADMIN peut ajouter des produits !");
            }
    
            // Trouver la boutique associée par son ID
            Boutique boutique = boutiqueRepository.findById(boutiqueId)
        .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));
        
    
            // Vérification de l'existence du produit par nom, prix et boutiqueId
            Produit existingProduit = produitRepository.findByNomAndBoutiqueId(produitRequest.getNom(), boutiqueId);
            if (existingProduit != null) {
                throw new RuntimeException("Un produit avec le même nom existe déjà dans cette boutique.");
            }

            // Vérification de l'existence du produit par codeBare et boutiqueId
            String codeBare = produitRequest.getCodeBare();
            if (codeBare != null && !codeBare.trim().isEmpty()) {
                Produit existingProduitCode = produitRepository.findByCodeBareAndBoutiqueId(codeBare.trim(), boutiqueId);
                if (existingProduitCode != null) {
                    throw new RuntimeException("Un produit avec le même codeBare existe déjà dans cette boutique.");
                }
            }
    
            // Vérifier si la catégorie existe dans la base de données 
            Categorie categorie = null;
            if (produitRequest.getCategorieId() != null) {
                categorie = categorieRepository.findById(produitRequest.getCategorieId())
                        .orElseThrow(() -> new RuntimeException("Catégorie non trouvée"));
            }
    
            // Vérifier si l'unité de mesure existe dans la base de données
            Unite unite = null;
            if (produitRequest.getUniteId() != null) {
                unite = uniteRepository.findById(produitRequest.getUniteId())
                        .orElseThrow(() -> new RuntimeException("Unité de mesure non trouvée"));
            }
    
            String codeGenerique = generateProductCode();

            // Mapper ProduitRequest à
            // Produit

            Produit produit = new Produit();
            produit.setNom(produitRequest.getNom());
            produit.setDescription(produitRequest.getDescription());
            produit.setPrixVente(produitRequest.getPrixVente());
            produit.setPrixAchat(produitRequest.getPrixAchat());
            produit.setQuantite(produitRequest.getQuantite() != null ? produitRequest.getQuantite() : 0);
            produit.setSeuilAlert(produitRequest.getSeuilAlert() != null ? produitRequest.getSeuilAlert() : 0);
            produit.setCategorie(categorie);
            produit.setUniteDeMesure(unite);
            produit.setCodeGenerique(codeGenerique);
            produit.setCodeBare(produitRequest.getCodeBare());
            produit.setPhoto(produitRequest.getPhoto());
            produit.setCreatedAt(LocalDateTime.now());
            produit.setLastUpdated(LocalDateTime.now());
            produit.setBoutique(boutique);
    
            // Enregistrer le produit dans la base de données
            Produit savedProduit = produitRepository.save(produit);
    
            // Si addToStock est vrai, ajouter le produit au stock
            if (addToStock) {
                Stock stock = new Stock();
            
                // Définir stockActuel comme la quantité du produit (ou 0 si non renseigné)
                stock.setStockActuel(produitRequest.getQuantite() != null ? produitRequest.getQuantite() : 0);
            
                // Initialiser stockApres avec la même valeur que stockActuel
                stock.setStockApres(stock.getStockActuel());
            
                // Initialiser quantiteAjoute à 0 (car pas encore de mise à jour de stock)
                stock.setQuantiteAjoute(0);
            
                stock.setBoutique(boutique);
                stock.setProduit(savedProduit);  // Associer le produit au stock
                stock.setCreatedAt(LocalDateTime.now());
                stock.setLastUpdated(LocalDateTime.now());
            
                // Si un seuil d'alerte est spécifié, on le copie dans le stock
                if (produitRequest.getSeuilAlert() != null) {
                    stock.setSeuilAlert(produitRequest.getSeuilAlert());
                }
            
                stockRepository.save(stock);
            
                savedProduit.setEnStock(true);
                produitRepository.save(savedProduit);
            }
            
    
            // Mapper l'entité Produit en DTO
            ProduitDTO produitDTO = mapToDTO(savedProduit);
    
            // Retourner le DTO
            return produitDTO;
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
    public Facture ajouterStock(Long boutiqueId, Map<Long, Integer> produitsQuantites, String description, HttpServletRequest request) {
        // Récupérer le token JWT depuis le header "Authorization"
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
    
        String jwtToken = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwtToken);
    
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    
        List<Produit> produits = new ArrayList<>();
    
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
    
            // Mettre à jour le produit et le stock
            produit.setQuantite(nouvelleQuantiteProduit);
            produitRepository.save(produit);
    
            stock.setStockActuel(nouvelleQuantiteProduit);
            stock.setQuantiteAjoute(quantiteAjoute);
            stock.setStockApres(nouvelleQuantiteProduit);
            stock.setLastUpdated(LocalDateTime.now());
            stockRepository.save(stock);
    
            // Enregistrer l'historique du stock
            StockHistory stockHistory = new StockHistory();
            stockHistory.setAction("Ajout sur quantité");
            stockHistory.setQuantite(quantiteAjoute);
            stockHistory.setStockAvant(stockAvant);
            stockHistory.setStockApres(nouvelleQuantiteProduit);
            stockHistory.setDescription(description);
            stockHistory.setCreatedAt(LocalDateTime.now());
            stockHistory.setStock(stock);
            stockHistory.setUser(user);
            stockHistoryRepository.save(stockHistory);
    
            produits.add(produit);
        }
    
        // Enregistrer une facture avec plusieurs produits
        return enregistrerFacture("AJOUTER", produits, produitsQuantites, description, user);
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

    Facture facture = enregistrerFacture("Réduction", produits, produitsQuantites, description, user);

    return new FactureDTO(facture);
}


      // Génère un numéro unique de facture
    private String generateNumeroFacture() {
        Long dernierId = factureRepository.count() + 1;
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "FAC-" + date + "-" + String.format("%04d", dernierId);
    }

    // Méthode pour enregistrer une facture
    public Facture enregistrerFacture(String type, List<Produit> produits, Map<Long, Integer> quantites, String description, User user) {
        Facture facture = new Facture();
        facture.setNumeroFacture(generateNumeroFacture());
        facture.setType(type);
        facture.setDescription(description);
        facture.setDateFacture(LocalDateTime.now());
        facture.setUser(user);

        // Associer la boutique à la facture (en supposant que tous les produits appartiennent à la même boutique)
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
    public ProduitDTO updateProduct(Long produitId, ProduitRequest produitRequest, boolean addToStock, HttpServletRequest request) {
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

    if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
        throw new RuntimeException("Seul un ADMIN peut modifier les produits !");
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
    if (produitRequest.getPhoto() != null) produit.setPhoto(produitRequest.getPhoto());

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


    

    return produitDTO;
}

    //Methoce Supprime le produit s’il n'est pas en stock
    public void deleteProduit(Long produitId) {
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé"));
    
        if (produit.getEnStock()) {
            throw new RuntimeException("Impossible de supprimer le produit car il est encore en stock !");
        }
    
        // Supprimer aussi son stock (au cas où il y en aurait encore)
        Stock stock = stockRepository.findByProduit(produit);
        if (stock != null) {
            stockRepository.delete(stock);
        }
    
        produitRepository.delete(produit);
        System.out.println("✅ Produit supprimé avec succès !");
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

    //Lister Produit par boutique
    public List<ProduitDTO> getProduitsParStock(Long boutiqueId) {
    // Récupérer les produits enStock = false
    List<Produit> produitsEnStockFalse = produitRepository.findByBoutiqueIdAndEnStockFalse(boutiqueId);

    // Récupérer les produits enStock = true
    List<Produit> produitsEnStockTrue = produitRepository.findByBoutiqueIdAndEnStockTrue(boutiqueId);

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

        return produitDTO;
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
        dto.setBoutiqueId(produit.getBoutique() != null ? produit.getBoutique().getId() : null); // Ajouter l'ID de la boutique
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
        dto.setEnStock(produit.getEnStock());
        dto.setCreatedAt(produit.getCreatedAt());
        dto.setLastUpdated(produit.getLastUpdated());
        dto.setNomCategorie(produit.getCategorie() != null ? produit.getCategorie().getNom() : null);
        dto.setNomUnite(produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getNom() : null);
        dto.setBoutiqueId(produit.getBoutique() != null ? produit.getBoutique().getId() : null); // Ajouter l'ID de la boutique
        return dto;
    }
    
    // Méthode pour récupérer tous les produits de toutes les boutiques d'une entreprise
    public List<ProduitDTO> getProduitsParEntreprise(Long entrepriseId) {
        // Récupérer tous les produits associés à l'entreprise via les boutiques
        List<Produit> produits = produitRepository.findByEntrepriseId(entrepriseId);
    
        // Mapper les entités Produit en ProduitDTO
        return produits.stream()
                .map(this::convertToProduitDTO)
                .collect(Collectors.toList());
    }
    

}
