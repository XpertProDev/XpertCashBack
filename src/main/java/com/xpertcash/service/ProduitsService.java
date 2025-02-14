package com.xpertcash.service;


import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.entity.CategoryProduit;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produits;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.CustomException;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.CategoryProduitRepository;
import com.xpertcash.repository.ProduitsRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.xpertcash.repository.UsersRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ProduitsService {

    @Autowired
    private ProduitsRepository produitsRepository;

    @Autowired
    private CategoryProduitRepository categoryProduitRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AuthorizationService authorizationService;

  

    @Autowired
    private DepenseService depenseService; // Service pour gérer les dépenses


    // Méthode pour Ajouter un produit (Admin seulement)
    public Produits ajouterProduit(Long userId, Produits produit) {
        // Vérifier si l'utilisateur existe
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));
    
        // Vérifier si l'utilisateur a les droits nécessaires (admin ou autre rôle autorisé)
        authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);
    
        // Vérifier que tous les champs requis sont renseignés
        if (produit.getNomProduit() == null || produit.getNomProduit().trim().isEmpty()) {
            throw new RuntimeException("Le nom du produit est obligatoire");
        }
        if (produit.getDescription() == null || produit.getDescription().trim().isEmpty()) {
            throw new RuntimeException("La description du produit est obligatoire");
        }
        if (produit.getPrix() == null || produit.getPrix() <= 0) {
            throw new RuntimeException("Le prix du produit doit être supérieur à 0");
        }
        if (produit.getQuantite() <= 0) {
            throw new RuntimeException("La quantité doit être supérieure à 0");
        }
        if (produit.getSeuil() <= 0) {
            throw new RuntimeException("Le seuil doit être supérieur à 0");
        }
        if (produit.getAlertSeuil() <= 0) {
            throw new RuntimeException("L'alerte de seuil doit être un nombre positif.");
        }
    
        // Vérifier que la catégorie du produit existe
        if (produit.getCategory() == null || produit.getCategory().getId() == null) {
            throw new RuntimeException("La catégorie du produit est obligatoire");
        }
    
        // Récupérer la catégorie depuis la base de données
        CategoryProduit category = categoryProduitRepository.findById(produit.getCategory().getId())
                .orElseThrow(() -> new RuntimeException("La catégorie avec l'ID " + produit.getCategory().getId() + " n'existe pas."));
    
        // Vérifier si le produit existe déjà dans cette catégorie
        Optional<Produits> existingProductOpt = produitsRepository.findByNomProduitAndCategory(produit.getNomProduit(), category);
    
        if (existingProductOpt.isPresent()) {
            // Si le produit existe déjà, mettre à jour la quantité
            Produits existingProduct = existingProductOpt.get();
            existingProduct.setQuantite(existingProduct.getQuantite() + produit.getQuantite());
            
            // Sauvegarder le produit mis à jour
            return produitsRepository.save(existingProduct);
        } else {
            // Si le produit n'existe pas, l'ajouter normalement
            produit.setCategory(category);
            return produitsRepository.save(produit);
        }
    }
    

    // Méthode pour Modifier un produit (Admin seulement)
    public Produits modifierProduit(Long userId, Long id, Produits produitModifie, MultipartFile imageFile) {
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));

        // Vérifier si l'utilisateur est un Admin
        authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);

        Produits produitExistant = produitsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé pour l'ID : " + id));

        // Mettre à jour les champs modifiables
        produitExistant.setNomProduit(produitModifie.getNomProduit());
        produitExistant.setDescription(produitModifie.getDescription());
        produitExistant.setPrix(produitModifie.getPrix());
        produitExistant.setQuantite(produitModifie.getQuantite());
        produitExistant.setSeuil(produitModifie.getSeuil());
        produitExistant.setAlertSeuil(produitModifie.getAlertSeuil());

        // Mise à jour de la catégorie si elle existe
        if (produitModifie.getCategory() != null && produitModifie.getCategory().getId() != null) {
            categoryProduitRepository.findById(produitModifie.getCategory().getId())
                    .orElseThrow(() -> new NotFoundException("Catégorie introuvable avec l'ID : " + produitModifie.getCategory().getId()));
            produitExistant.setCategory(produitModifie.getCategory());
        }


        return produitsRepository.save(produitExistant);
    }

    // Méthode pour récupérer tous les produits
    public List<Produits> getAllProduits() {
        return produitsRepository.findAll(); // Récupère tous les produits depuis la base de données
    }
    
    // Méthode pour récupérer un produit par son ID
    public Produits getProduitById(Long id) {
        return produitsRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé pour l'ID : " + id)); // Récupère un produit par son ID
    }

    // Méthode pour supprimer un produit (Admin seulement)
    public String deleteProduit(Long userId, Long id) {
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));

        // Vérifier si l'utilisateur est un Admin
        authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);

        if (produitsRepository.findById(id).isPresent()) {
            produitsRepository.deleteById(id);
            return "Succès : produit supprimé.";
        } else {
            throw new NotFoundException("Produit non trouvé pour l'ID : " + id);
        }
    }

    // Méthode pour approvisionner le stock (Comptable seulement)
    public String approvisionnerStock(Long userId, Long produitId, int quantite) {
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));

        // Vérifier si l'utilisateur est un Comptable
        authorizationService.checkPermission(user, PermissionType.APPROVISIONNER_STOCK);

        Produits produit = produitsRepository.findById(produitId)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé pour l'ID : " + produitId));

        // Approvisionner le stock
        int ancienneQuantite = produit.getQuantite();
        produit.setQuantite(ancienneQuantite + quantite);

        // Enregistrer la dépense
        double montantDepense = quantite * produit.getPrix();  // Montant basé sur la quantité et le prix unitaire
        depenseService.enregistrerDepense(montantDepense, "Approvisionnement de stock pour le produit " + produit.getNomProduit(), user);

        produitsRepository.save(produit);

        return "Approvisionnement effectué avec succès.";
    }
}