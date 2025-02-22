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
import java.util.Map;
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
            LocalDateTime expirationDate = user.getCreatedAt().plusHours(24);
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

            // Vérification du codebar seulement s'il est renseigné
            if (produit.getCodebar() != null && !produit.getCodebar().trim().isEmpty()) {
                Optional<Produits> existingProductByCodebar = produitsRepository.findByCodebar(produit.getCodebar());
                if(existingProductByCodebar.isPresent()){
                    throw new RuntimeException("Un produit avec ce code barre existe déjà.");
                }
            }

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
        
            stock.setProduit(produit);
        
            stock.setQuantite(quantite != null ? quantite : 0);
        
            // Associe la catégorie du produit au stock (en supposant que la catégorie existe)
            if (produit.getCategory() != null) {
                stock.setCategory(produit.getCategory());
            }
        
            // Date actuelle d'ajout au stock
            stock.setDateAjout(LocalDateTime.now());
        
            stockRepository.save(stock);
        }

    // Méthode pour générer un code unique pour chaque produit
    private String generateProductCode() {
        String randomCode = String.format("%05d", (int)(Math.random() * 100000));

        return "P-" + randomCode;
    }

   //Methode Listing Entreprise Product
   public List<Produits> listerProduitsEntreprise(Long userId) {
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));
    
        if (user.getEntreprise() == null) {
            throw new RuntimeException("L'utilisateur ne fait partie d'aucune entreprise.");
        }
    
        boolean isAdmin = user.getRole() != null && user.getRole().getName().equals(RoleType.ADMIN);
    
        // Vérifier la condition de l'activation du compte
        LocalDateTime expirationDate = user.getCreatedAt().plusHours(24);  // Utilisation de l'exemple de délai de 5 minutes
        boolean withinTimeLimit = LocalDateTime.now().isBefore(expirationDate);
    
        if (isAdmin) {
            if (!user.isActivatedLien() && !withinTimeLimit) {
                throw new RuntimeException("Votre compte administrateur n'est pas activé et la période de connexion temporaire est expirée.");
            }
        } else {
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
    
    //Methode Update Produit
    public Produits modifierProduit(Long userId, Long produitId, Map<String, Object> updates) {
        // Vérifier si le produit existe
        Produits produit = produitsRepository.findById(produitId)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé"));
    
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));
        authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);
    
        // Vérifier si le codeProduit ou la catégorie sont dans les mises à jour
        if (updates.containsKey("codeProduit")) {
            throw new RuntimeException("Le code du produit ne peut pas être modifié.");
        }
        if (updates.containsKey("category")) {
            throw new RuntimeException("La catégorie du produit ne peut pas être modifiée.");
        }
    
        // ce qui peuvent etre modifier
        updates.forEach((key, value) -> {
            switch (key) {
                case "nomProduit":
                    produit.setNomProduit((String) value);
                    break;
                case "description":
                    produit.setDescription((String) value);
                    break;
                case "prix":
                    produit.setPrix(Double.valueOf(value.toString()));
                    break;
                case "prixAchat":
                    produit.setPrixAchat(Double.valueOf(value.toString()));
                    break;
                case "quantite":
                    produit.setQuantite(Integer.valueOf(value.toString()));
                    break;
                case "alertSeuil":
                    produit.setAlertSeuil(Integer.valueOf(value.toString()));
                    break;
                case "uniteMesure":
                    if (value instanceof Map) {
                        Map<String, String> uniteMesureMap = (Map<String, String>) value;
                        String nomUnite = uniteMesureMap.get("nomUnite");
                        UniteMesure uniteMesure = uniteMesureRepository.findByNomUnite(nomUnite)
                                .orElseGet(() -> {
                                    UniteMesure newUnite = new UniteMesure();
                                    newUnite.setNomUnite(nomUnite);
                                    return uniteMesureRepository.save(newUnite);
                                });
                        produit.setUniteMesure(uniteMesure);
                    }
                    break;
            }
        });
    
        return produitsRepository.save(produit);
    }
    
     
    
}