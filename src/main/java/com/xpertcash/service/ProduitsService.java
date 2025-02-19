package com.xpertcash.service;


import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.entity.CategoryProduit;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produits;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.UniteMesure;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.CategoryProduitRepository;
import com.xpertcash.repository.ProduitsRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.UniteMesureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xpertcash.repository.UsersRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProduitsService {

    @Autowired

    private ProduitsRepository produitsRepository;

    @Autowired
    private UniteMesureRepository uniteMesureRepository;

    @Autowired
    private CategoryProduitRepository categoryProduitRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private StockRepository stockRepository;



        // Méthode pour Ajouter un produit (Admin seulement)
        public Produits ajouterProduit(Long userId, Produits produit) {
            // Vérification de l'utilisateur
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));
        
            if (user.getEntreprise() == null) {
                throw new RuntimeException("L'utilisateur ne fait partie d'aucune entreprise.");
            }
        
            User admin = user.getEntreprise().getAdmin();
            LocalDateTime expirationDate = user.getCreatedAt().plusMinutes(5);
            boolean withinTimeLimit = LocalDateTime.now().isBefore(expirationDate);
        
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
        
            // Vérification des permissions
            authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);
        
            // Vérifications des champs obligatoires
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
            if (produit.getUniteMesure() == null || produit.getUniteMesure().getNomUnite().trim().isEmpty()) {
                throw new RuntimeException("L'unité de mesure est obligatoire.");
            }
            if (produit.getCategory() == null || produit.getCategory().getId() == null) {
                throw new RuntimeException("La catégorie du produit est obligatoire.");
            }
        
            // Récupérer la catégorie depuis la base de données
            CategoryProduit category = categoryProduitRepository.findById(produit.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("La catégorie avec l'ID " + produit.getCategory().getId() + " n'existe pas."));
        
            // Récupérer ou créer l'unité de mesure
            String nomUnite = produit.getUniteMesure().getNomUnite(); // On s'assure d'avoir une String
            UniteMesure uniteMesure = uniteMesureRepository.findByNomUnite(nomUnite)
                    .orElseGet(() -> {
                        UniteMesure newUniteMesure = new UniteMesure();
                        newUniteMesure.setNomUnite(nomUnite);
                        return uniteMesureRepository.save(newUniteMesure);
                    });
        
            // Vérifier si un produit similaire existe déjà dans cette catégorie avec la même unité de mesure
            Optional<Produits> existingProductOpt = produitsRepository.findByNomProduitAndCategoryAndUniteMesure(
                    produit.getNomProduit(), category, uniteMesure);
        
            if (existingProductOpt.isPresent()) {
                throw new RuntimeException("Impossible d'ajouter ce produit. Un produit du même nom avec cette unité de mesure existe déjà dans la catégorie.");
            }
        
            // Générer un code unique pour le produit
            String codeProduit = generateProductCode();
            produit.setCodeProduit(codeProduit);
        
            // Associer l'unité de mesure au produit
            produit.setUniteMesure(uniteMesure);
        
            // Ajouter le produit normalement
            produit.setCategory(category);

            // Sauvegarder le produit
        Produits savedProduit = produitsRepository.save(produit);

            // Ajouter le produit au stock
            ajouterStock(savedProduit, produit.getQuantite());
            return produitsRepository.save(produit);
        }


         // Méthode pour ajouter un produit au stock
         private void ajouterStock(Produits produit, Integer quantite) {
            // Vérifie si le produit existe déjà dans le stock
            Stock stock = stockRepository.findByProduitId(produit.getId())
                    .orElse(new Stock());
        
            // Associe le produit au stock
            stock.setProduit(produit);
        
            // Si une quantité est donnée, on l'utilise, sinon on met 0
            stock.setQuantite(quantite != null ? quantite : 0);
        
            // Associe la catégorie du produit au stock (en supposant que la catégorie existe)
            if (produit.getCategory() != null) {
                stock.setCategory(produit.getCategory());
            }
        
            // Date actuelle d'ajout au stock
            stock.setDateAjout(LocalDateTime.now());
        
            // Sauvegarde du stock dans la base de données
            stockRepository.save(stock);
        }
        
            

        // Méthode pour générer un code unique pour chaque produit
        private String generateProductCode() {
            // Générer un nombre aléatoire entre 10000 et 99999 (5 chiffres)
            String randomCode = String.format("%05d", (int)(Math.random() * 100000));
            
            // Retourner le code produit au format "PROD-xxxxx"
            return "P-" + randomCode;
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