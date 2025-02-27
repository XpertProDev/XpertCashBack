package com.xpertcash.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            }
    
            return savedProduit;
        } catch (RuntimeException e) {
            System.err.println("Erreur lors de la création du produit : " + e.getMessage());
            throw new RuntimeException("Une erreur est survenue lors de la création du produit : " + e.getMessage(), e);
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

    @Transactional
    public Produit updateProduct(Long produitId, Produit produitRequest, boolean addToStock, HttpServletRequest request) {
    // Vérifier si l'admin est connecté et possède les droits nécessaires
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    // Extraire le token sans le "Bearer "
    token = token.replace("Bearer ", "");

    Long adminId = null;
    try {
        // Décrypter le token pour obtenir l'ID de l'admin
        adminId = jwtUtil.extractUserId(token);
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'admin depuis le token", e);
    }

    // Récupérer l'admin par l'ID extrait du token
    User admin = usersRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

    // Vérifier que l'Admin est bien un ADMIN
    if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
        throw new RuntimeException("Seul un ADMIN peut modifier les produits !");
    }

    // Récupérer le produit à modifier
    Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new RuntimeException("Produit non trouvé"));

    // Modifier les propriétés du produit
    produit.setNom(produitRequest.getNom());
    produit.setPrixVente(produitRequest.getPrixVente());
    produit.setPrixAchat(produitRequest.getPrixAchat());
    produit.setQuantite(produitRequest.getQuantite());
    produit.setSeuilAlert(produitRequest.getSeuilAlert());
    produit.setCategorie(produitRequest.getCategorie());
    produit.setUniteDeMesure(produitRequest.getUniteDeMesure());
    produit.setCodeGenerique(produitRequest.getCodeGenerique());
    produit.setCodeBare(produitRequest.getCodeBare());
    produit.setPhoto(produitRequest.getPhoto());
    produit.setEnStock(addToStock);  // Si le produit doit aller en stock ou pas

    produit.setCreatedAt(produit.getCreatedAt()); // Garder la date de création intacte
    produitRepository.save(produit);

    // Si l'admin a choisi d'ajouter ce produit au stock, alors ajouter le produit au stock de la boutique
    if (addToStock) {
        // Vérifier si le produit est déjà dans le stock
        Stock stock = stockRepository.findByProduit(produit);
        if (stock == null) {
            // Si le produit n'est pas dans le stock, l'ajouter
            Stock newStock = new Stock();
            newStock.setProduit(produit);
            newStock.setQuantite(produitRequest.getQuantite());
            newStock.setBoutique(produit.getBoutique());
            newStock.setCreatedAt(LocalDateTime.now());
            newStock.setLastUpdated(LocalDateTime.now());
            stockRepository.save(newStock);
        } else {
            // Si le produit est déjà dans le stock, mettre à jour la quantité
            stock.setQuantite(produitRequest.getQuantite());
            stock.setLastUpdated(LocalDateTime.now());
            stockRepository.save(stock);
        }
    }

    return produit;
}

   
}
