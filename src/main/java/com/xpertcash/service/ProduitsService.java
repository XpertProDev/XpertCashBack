package com.xpertcash.service;


import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.entity.CategoryProduit;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produits;
import com.xpertcash.entity.Role;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.UniteMesure;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.CustomException;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.CategoryProduitRepository;
import com.xpertcash.repository.ProduitsRepository;
import com.xpertcash.repository.RoleRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.xpertcash.repository.UsersRepository;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Collections;
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
    private DepenseService depenseService; 


        // Méthode pour Ajouter un produit (Admin seulement)

        public Produits ajouterProduit(Long userId, Produits produit) {
            // Vérifier si l'utilisateur existe
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));
        
            // Vérifier si l'utilisateur appartient à une entreprise
            if (user.getEntreprise() == null) {
                throw new RuntimeException("L'utilisateur ne fait partie d'aucune entreprise.");
            }
        
            // Récupérer l'admin de l'entreprise
            User admin = user.getEntreprise().getAdmin();
        
            // Vérification si le compte a été créé récemment (dans les 24h ou 5 minutes pour le test)
            LocalDateTime expirationDate = user.getCreatedAt().plusMinutes(5);
            boolean withinTimeLimit = LocalDateTime.now().isBefore(expirationDate);
        
            // Vérification selon si l'utilisateur est un admin ou un employé
            boolean isAdmin = user.getRole() != null && user.getRole().getName().equals(RoleType.ADMIN);
        
            if (isAdmin) {
                if (!user.isActivatedLien() && !withinTimeLimit) {
                    throw new RuntimeException("Votre compte administrateur n'est pas activé et la période de connexion temporaire est expirée.");
                }
            } else {
                if (!admin.isActivatedLien() && !withinTimeLimit) {
                    throw new RuntimeException("Votre compte est désactivé car votre administrateur n'a pas activé son compte.");
                }
            }
        
            // Vérification des permissions de l'utilisateur
            authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);
        
            // Vérification des champs obligatoires
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
                throw new RuntimeException("L'unité de mesure est obligatoire (UNITE, KG, M, L, CARTON, etc.).");
            }
            if (produit.getCategory() == null || produit.getCategory().getId() == null) {
                throw new RuntimeException("La catégorie du produit est obligatoire.");
            }
        
            // Récupérer la catégorie depuis la base de données
            CategoryProduit category = categoryProduitRepository.findById(produit.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("La catégorie avec l'ID " + produit.getCategory().getId() + " n'existe pas."));
        
            // Vérifier si un produit similaire existe déjà dans cette catégorie (même nom de produit et même unité de mesure)
            Optional<Produits> existingProductOpt = produitsRepository.findByNomProduitAndCategoryAndUniteMesure(
                    produit.getNomProduit(), category, produit.getUniteMesure());
        
            if (existingProductOpt.isPresent()) {
                throw new RuntimeException("Impossible d'ajouter ce produit. Un produit du même nom avec cette unité de mesure existe déjà dans la catégorie.");
            }
        
            // Générer un code unique pour le produit
            String codeProduit = generateProductCode();
            produit.setCodeProduit(codeProduit);
        
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
        


       //Listing Entreprise Product

       public List<Produits> listerProduitsEntreprise(Long userId) {
        // Vérifier si l'utilisateur existe
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));
    
        // Vérifier si l'utilisateur appartient à une entreprise
        if (user.getEntreprise() == null) {
            throw new RuntimeException("L'utilisateur ne fait partie d'aucune entreprise.");
        }
    
        // Vérifier si l'utilisateur est un admin
        boolean isAdmin = user.getRole() != null && user.getRole().getName().equals(RoleType.ADMIN);
    
        // Vérifier la condition de l'activation du compte
        LocalDateTime expirationDate = user.getCreatedAt().plusMinutes(5);  // Utilisation de l'exemple de délai de 5 minutes
        boolean withinTimeLimit = LocalDateTime.now().isBefore(expirationDate);
    
        // Vérification selon si l'utilisateur est un admin ou non
        if (isAdmin) {
            // Si l'admin n'est pas activé, afficher un message d'erreur
            if (!user.isActivatedLien() && !withinTimeLimit) {
                throw new RuntimeException("Votre compte administrateur n'est pas activé et la période de connexion temporaire est expirée.");
            }
        } else {
            // Si l'utilisateur n'est pas un admin (employé), vérifier si le compte de l'admin est activé
            if (!user.getEntreprise().getAdmin().isActivatedLien()) {
                if (!withinTimeLimit) {
                    throw new RuntimeException("Votre compte est désactivé car votre administrateur n'a pas activé son compte.");
                }
            }
        }
    
        // Récupérer l'entreprise de l'utilisateur
        Entreprise entreprise = user.getEntreprise();
    
        // Récupérer toutes les catégories de produits associées à l'entreprise
        List<CategoryProduit> categories = categoryProduitRepository.findByEntreprise(entreprise);
        
        if (categories.isEmpty()) {
            throw new RuntimeException("Aucune catégorie trouvée pour cette entreprise.");
        }
    
        // Récupérer tous les produits associés aux catégories de l'entreprise
        List<Produits> produits = produitsRepository.findByCategoryIn(categories);
    
        return produits;
    }
    
     
    
}