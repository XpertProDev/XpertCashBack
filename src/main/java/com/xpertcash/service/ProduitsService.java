package com.xpertcash.service;


import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.entity.*;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

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

    @Autowired
    private MagasinRepository magasinRepository;

    // Méthode pour Ajouter un produit (Admin seulement)
    public Produits ajouterProduit(Long userId, Long magasinId, Produits produit) {
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
            if (existingProductByCodebar.isPresent()) {
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

        // Vérifier que le magasin existe et qu'il appartient à l'entreprise de l'utilisateur
        Magasin magasin = magasinRepository.findById(magasinId)
                .orElseThrow(() -> new RuntimeException("Le magasin avec l'ID " + magasinId + " n'existe pas."));

        if (!magasin.getEntreprise().equals(user.getEntreprise())) {
            throw new RuntimeException("Le magasin ne fait pas partie de votre entreprise.");
        }

        // Générer un code unique pour le produit
        String codeProduit = generateProductCode();
        produit.setCodeProduit(codeProduit);

        // Associer l'unité de mesure et la catégorie au produit
        produit.setUniteMesure(uniteMesure);
        produit.setCategory(category);

        // Associer le produit au magasin sélectionné
        produit.setMagasin(magasin);

        // Sauvegarder le produit
        Produits savedProduit = produitsRepository.save(produit);

        // Ajouter le produit au stock du magasin
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

       if (produits.isEmpty()) {
           throw new RuntimeException("Aucun produit avec un magasin trouvé pour cette entreprise.");
       }

        return produits;
    }

    // Service Produits
    public Produits modifierProduit(Long userId, Long produitId, Produits produitModif) {
        // Vérification de l'utilisateur
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));
        // Vérification des permissions
        authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);

        // Récupération du produit existant
        Produits existingProduit = produitsRepository.findById(produitId)
                .orElseThrow(() -> new NotFoundException("Produit non trouvé."));

        // Mise à jour du nom du produit s'il est fourni
        if (produitModif.getNomProduit() != null && !produitModif.getNomProduit().trim().isEmpty()) {
            existingProduit.setNomProduit(produitModif.getNomProduit());
        }

        // Mise à jour de la description
        if (produitModif.getDescription() != null && !produitModif.getDescription().trim().isEmpty()) {
            existingProduit.setDescription(produitModif.getDescription());
        }

        // Mise à jour du prix
        if (produitModif.getPrix() != null && produitModif.getPrix() > 0) {
            existingProduit.setPrix(produitModif.getPrix());
        }

        // Mise à jour du prix d'achat
        if (produitModif.getPrixAchat() != null && produitModif.getPrixAchat() > 0) {
            existingProduit.setPrixAchat(produitModif.getPrixAchat());
        }

        // Mise à jour de la quantité
        if (produitModif.getQuantite() > 0) {
            existingProduit.setQuantite(produitModif.getQuantite());
            // Mise à jour du stock
            mettreAJourStock(existingProduit, produitModif.getQuantite());
        } else if (produitModif.getQuantite() != 0) {
            throw new RuntimeException("La quantité doit être supérieure à 0.");
        }


        // Mise à jour de l'alerte de seuil
        if (produitModif.getAlertSeuil() > 0) {
            existingProduit.setAlertSeuil(produitModif.getAlertSeuil());
        } else if (produitModif.getAlertSeuil() != 0) {
            throw new RuntimeException("L'alerte de seuil doit être un nombre positif.");
        }

        // Mise à jour de l'unité de mesure si fournie
        if (produitModif.getUniteMesure() != null &&
                produitModif.getUniteMesure().getNomUnite() != null &&
                !produitModif.getUniteMesure().getNomUnite().trim().isEmpty()) {

            String nomUnite = produitModif.getUniteMesure().getNomUnite();
            UniteMesure uniteMesure = uniteMesureRepository.findByNomUnite(nomUnite)
                    .orElseGet(() -> {
                        UniteMesure newUniteMesure = new UniteMesure();
                        newUniteMesure.setNomUnite(nomUnite);
                        return uniteMesureRepository.save(newUniteMesure);
                    });
            existingProduit.setUniteMesure(uniteMesure);
        }

        // Mise à jour de la photo si une nouvelle URL est fournie
        if (produitModif.getPhoto() != null && !produitModif.getPhoto().trim().isEmpty()) {
            System.out.println("Mise à jour de la photo avec URL : " + produitModif.getPhoto());
            existingProduit.setPhoto(produitModif.getPhoto());
        }

        existingProduit.setCreatedAt(LocalDateTime.now());

        // Sauvegarde et retour du produit modifié
        Produits savedProduit = produitsRepository.save(existingProduit);
        //System.out.println("Produit sauvegardé avec photo : " + savedProduit.getPhoto());
        return savedProduit;
    }

    private void mettreAJourStock(Produits produit, int nouvelleQuantite) {

        Stock stock = produit.getStock();
        if (stock != null) {
            stock.setQuantite(nouvelleQuantite);
            stockRepository.save(stock);
        } else {
            // Si le produit n'a pas encore de stock, tu pourrais en créer un
            Stock newStock = new Stock();
            newStock.setProduit(produit);
            newStock.setDateAjout(LocalDateTime.now());
            newStock.setQuantite(nouvelleQuantite);
            stockRepository.save(newStock);
        }
    }


    //Methode  types de produits dans chaque magasin La quantité totale.
    public List<Map<String, Object>> getCountAndTotalQuantitiesByMagasin() {
        List<Object[]> results = produitsRepository.countAndSumQuantitiesByMagasin();
        List<Map<String, Object>> magasinStats = new ArrayList<>();

        for (Object[] result : results) {
            Map<String, Object> data = new HashMap<>();
            data.put("magasinId", result[0]);
            data.put("nombreProduits", result[1]);
            data.put("quantiteTotale", result[2]);
            magasinStats.add(data);
        }

        return magasinStats;
    }



}