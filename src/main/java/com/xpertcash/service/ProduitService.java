package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xpertcash.DTOs.ProduitDTO;
import com.xpertcash.DTOs.ProduitRequest;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Categorie;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.Unite;
import com.xpertcash.entity.User;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.CategorieRepository;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.UniteRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;


@Service
public class ProduitService {

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

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
                    .orElseThrow(() -> new RuntimeException("Boutique avec l'ID " + boutiqueId + " non trouvée"));
    
            // Vérification de l'existence du produit par nom, prix et boutiqueId
            Produit existingProduit = produitRepository.findByNomAndPrixVenteAndBoutiqueId(produitRequest.getNom(), produitRequest.getPrixVente(), boutiqueId);
            if (existingProduit != null) {
                throw new RuntimeException("Un produit avec le même nom et prix existe déjà dans cette boutique.");
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
    
            // Mapper ProduitRequest à Produit
            Produit produit = new Produit();
            produit.setNom(produitRequest.getNom());
            produit.setDescription(produitRequest.getDescription());
            produit.setPrixVente(produitRequest.getPrixVente());
            produit.setPrixAchat(produitRequest.getPrixAchat());
            produit.setQuantite(produitRequest.getQuantite());
            produit.setSeuilAlert(produitRequest.getSeuilAlert());
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
                stock.setQuantite(produitRequest.getQuantite());
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
            newStock.setQuantite(produit.getQuantite());  // Quantité du produit dans le stock
            newStock.setBoutique(produit.getBoutique());  // Associer la boutique au stock
            newStock.setCreatedAt(LocalDateTime.now());  // Date de création du stock
            newStock.setLastUpdated(LocalDateTime.now());  // Date de dernière mise à jour

            // Ajouter le seuil d'alerte dans le stock
            if (produitRequest.getSeuilAlert() != null) {
                newStock.setSeuilAlert(produitRequest.getSeuilAlert());
            } else {
                newStock.setSeuilAlert(produit.getSeuilAlert());
            }

            stockRepository.save(newStock);  // Sauvegarde du nouveau stock
        } else {
            // Si le stock existe déjà, mise à jour des informations du stock
            stock.setQuantite(produit.getQuantite());  // Mettre à jour la quantité
            stock.setLastUpdated(LocalDateTime.now());  // Mettre à jour la date de dernière mise à jour

            // Mettre à jour le seuil d'alerte dans le stock si spécifié dans la requête
            if (produitRequest.getSeuilAlert() != null) {
                stock.setSeuilAlert(produitRequest.getSeuilAlert());
            }

            stockRepository.save(stock);  // Sauvegarde du stock mis à jour
        }

        produit.setEnStock(true);  // Le produit est en stock
    } else {
        // Si addToStock est false, on met à jour le produit et on supprime le stock si nécessaire
        Stock stock = stockRepository.findByProduit(produit);

        if (stock != null) {
            // Si le stock existe, on le supprime
            stockRepository.delete(stock);
        }

        produit.setEnStock(false);  // Le produit n'est plus en stock
    }

    // Mettre à jour le produit pour refléter l'état hors stock ou en stock
    produitRepository.save(produit);

    // Mapper Produit vers ProduitDTO pour la réponse
    ProduitDTO produitDTO = new ProduitDTO();
    produitDTO.setId(produit.getId());
    produitDTO.setNom(produit.getNom());
    produitDTO.setPrixVente(produit.getPrixVente());
    produitDTO.setPrixAchat(produit.getPrixAchat());
    produitDTO.setQuantite(produit.getQuantite());
    produitDTO.setSeuilAlert(produit.getSeuilAlert());
    produitDTO.setCategorieId(produit.getCategorie().getId());
    produitDTO.setUniteId(produit.getUniteDeMesure().getId());
    produitDTO.setCodeBare(produit.getCodeBare());
    produitDTO.setPhoto(produit.getPhoto());
    produitDTO.setEnStock(produit.getEnStock());

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
        produitDTO.setPrixAchat(produit.getPrixAchat());
        produitDTO.setQuantite(produit.getQuantite());
        produitDTO.setSeuilAlert(produit.getSeuilAlert());
        produitDTO.setCategorieId(produit.getCategorie() != null ? produit.getCategorie().getId() : null);
        produitDTO.setUniteId(produit.getUniteDeMesure() != null ? produit.getUniteDeMesure().getId() : null);
        produitDTO.setCodeBare(produit.getCodeBare());
        produitDTO.setPhoto(produit.getPhoto());
        produitDTO.setEnStock(produit.getEnStock());
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
    
}
