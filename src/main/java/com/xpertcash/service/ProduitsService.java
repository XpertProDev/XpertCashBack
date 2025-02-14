package com.xpertcash.service;


import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.entity.CategoryProduit;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produits;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.UniteMesure;
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

import java.text.SimpleDateFormat;
import java.util.Date;
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

            // Vérification des champs requis
            if (produit.getNomProduit() == null || produit.getNomProduit().trim().isEmpty()) {
                throw new RuntimeException("Le nom du produit est obligatoire.");
            }
            if (produit.getDescription() == null || produit.getDescription().trim().isEmpty()) {
                throw new RuntimeException("La description du produit est obligatoire.");
            }
            if (produit.getPrix() == null || produit.getPrix() <= 0) {
                throw new RuntimeException("Le prix du produit doit être supérieur à 0.");
            }
            if (produit.getPrixAchat() == null || produit.getPrixAchat() <= 0) {
                throw new RuntimeException("Le prix d'achat doit être supérieur à 0.");
            }
            if (produit.getQuantite() <= 0) {
                throw new RuntimeException("La quantité doit être supérieure à 0.");
            }
            if (produit.getAlertSeuil() <= 0) {
                throw new RuntimeException("L'alerte de seuil doit être un nombre positif.");
            }
            if (produit.getUniteMesure() == null) {
                throw new RuntimeException("L'unité de mesure est obligatoire (UNITE, KG, M, L).");
            }

            // Vérifier que la catégorie du produit existe
            if (produit.getCategory() == null || produit.getCategory().getId() == null) {
                throw new RuntimeException("La catégorie du produit est obligatoire.");
            }

            // Récupérer la catégorie depuis la base de données
            CategoryProduit category = categoryProduitRepository.findById(produit.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("La catégorie avec l'ID " + produit.getCategory().getId() + " n'existe pas."));

                    // Générer un code unique pour le produit
                    String codeProduit = generateProductCode();
                    produit.setCodeProduit(codeProduit); 

            // Vérifier si un produit existe déjà dans cette catégorie avec la même unité de mesure
            List<Produits> produitsInCategory = produitsRepository.findByCategory(category);

            // Vérifier si la catégorie contient déjà un produit avec une unité de mesure spécifique
            if (!produitsInCategory.isEmpty()) {
                UniteMesure existingUniteMesure = produitsInCategory.get(0).getUniteMesure();

                // Si un produit existe déjà dans cette catégorie avec une unité de mesure, vérifier la compatibilité de l'unité de mesure
                if (!existingUniteMesure.equals(produit.getUniteMesure())) {
                    throw new RuntimeException("Impossible d'ajouter ce produit. Tous les produits dans cette catégorie doivent avoir la même unité de mesure.");
                }
            }

            // Vérifier si un produit similaire existe déjà dans cette catégorie (même nom de produit et même unité de mesure)
            Optional<Produits> existingProductOpt = produitsRepository.findByNomProduitAndCategory(produit.getNomProduit(), category);

            if (existingProductOpt.isPresent()) {
                Produits existingProduct = existingProductOpt.get();

                // Vérifier que l'unité de mesure du produit existant correspond à celle du nouveau produit
                if (!existingProduct.getUniteMesure().equals(produit.getUniteMesure())) {
                    throw new RuntimeException("Impossible d'ajouter ce produit. L'unité de mesure ne correspond pas ("
                        + existingProduct.getUniteMesure() + " attendu, mais reçu : " + produit.getUniteMesure() + ").");
                }

                // Si le produit existe déjà avec la même unité de mesure, refuser l'ajout
                throw new RuntimeException("Impossible d'ajouter ce produit. Un produit similaire existe déjà dans la catégorie avec cette unité de mesure.");
            }

            // Ajouter le nouveau produit normalement
            produit.setCategory(category);
            return produitsRepository.save(produit);
        }

        // Méthode pour générer un code unique pour chaque produit
        private String generateProductCode() {
            // Générer un nombre aléatoire entre 10000 et 99999 (5 chiffres)
            String randomCode = String.format("%05d", (int)(Math.random() * 100000));
            
            // Retourner le code produit au format "PROD-xxxxx"
            return "PROD-" + randomCode;
        }
        
    
}