package com.xpertcash.service;

import java.time.LocalDateTime;

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


    // 1. Ajouter un produit à la liste sans le stock
    public Produit createProduit(HttpServletRequest request, Long boutiqueId, ProduitRequest produitRequest, boolean addToStock) {
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
            // Associer la boutique au produit
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
                stockRepository.save(stock);

                savedProduit.setEnStock(true);
                produitRepository.save(savedProduit);
            }
            
            return savedProduit;
        } catch (RuntimeException e) {
            System.err.println("Erreur lors de la création du produit : " + e.getMessage());
            throw new RuntimeException(e.getMessage(), e);
        }
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
        if (produitRequest.getPrixVente() != null) produit.setPrixVente(produitRequest.getPrixVente());
        if (produitRequest.getPrixAchat() != null) produit.setPrixAchat(produitRequest.getPrixAchat());
        if (produitRequest.getQuantite() != null) produit.setQuantite(produitRequest.getQuantite());
        if (produitRequest.getSeuilAlert() != null) produit.setSeuilAlert(produitRequest.getSeuilAlert());
        if (produitRequest.getCodeBare() != null) produit.setCodeBare(produitRequest.getCodeBare());
        if (produitRequest.getPhoto() != null) produit.setPhoto(produitRequest.getPhoto());
        
        // Mise à jour de 'enStock' en fonction du paramètre 'addToStock'
        if (produitRequest.getEnStock() != null) {
            produit.setEnStock(produitRequest.getEnStock());
        } else {
            produit.setEnStock(addToStock); // Si enStock n'est pas fourni, mettre à jour enStock avec 'addToStock'
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
    
        produitRepository.save(produit);
    
        // Si addToStock est vrai, il faut transférer les produits dans le stock
        if (addToStock) {
            Stock stock = stockRepository.findByProduit(produit);
            
            if (stock == null) {
                // Si le produit n'a pas encore de stock, on crée un nouveau stock avec la quantité du produit
                Stock newStock = new Stock();
                newStock.setProduit(produit);
                newStock.setQuantite(produitRequest.getQuantite() != null ? produitRequest.getQuantite() : 0); // Ajouter une valeur par défaut si quantite est null
                newStock.setBoutique(produit.getBoutique());
                newStock.setCreatedAt(LocalDateTime.now());
                newStock.setLastUpdated(LocalDateTime.now());
                stockRepository.save(newStock);
            } else {
                // Si le produit existe déjà dans le stock et qu'il n'était pas encore en stock, on met à jour la quantité
                if (!produit.getEnStock()) {
                    stock.setQuantite(produitRequest.getQuantite() != null ? produitRequest.getQuantite() : 0); // On transfert la quantité du produit dans le stock
                    stock.setLastUpdated(LocalDateTime.now());
                    stockRepository.save(stock);
                }
            }
            
            produit.setEnStock(true); // On s'assure que le produit est en stock
        } else {
            // Si addToStock est false, on retire le produit du stock
            Stock stock = stockRepository.findByProduit(produit);
            if (stock != null) {
                stock.setQuantite(0);  // Mettre la quantité du stock à 0
                stock.setLastUpdated(LocalDateTime.now());
                stockRepository.save(stock);
            }
            
            produit.setEnStock(false); // Le produit n'est plus en stock
        }
    
        // Mapper Produit vers ProduitDTO
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
    
 
}
