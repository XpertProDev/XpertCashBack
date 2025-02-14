package com.xpertcash.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.CategoryProduit;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produits;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.CategoryProduitRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.ProduitsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/auth")


public class ProduitsController {

    @Autowired
    private ProduitsService produitsService;

    @Autowired
    private AuthorizationService authorizationService; // Service pour vérifier les permissions
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private CategoryProduitRepository categoryProduitRepository;
    @Autowired
    private JwtUtil jwtUtil;

    
        // Méthode pour Ajouter un produit
            // Méthode pour Ajouter un produit
@PostMapping("/add/produit")
public ResponseEntity<?> ajouterProduit(
        @RequestHeader("Authorization") String token,  // Récupération du token JWT
        @RequestBody Produits produit  // Récupération du produit directement dans le body
) {
    try {
        // Log d'entrée pour le débogage
        System.out.println("Tentative d'ajout du produit : " + produit.toString());

        // Extraire l'ID de l'utilisateur à partir du token JWT
        String jwtToken = token.substring(7);  // Enlever le "Bearer " du début du token
        Long userId = jwtUtil.extractUserId(jwtToken);  // Extraire l'ID de l'utilisateur

        // Récupérer l'utilisateur via son ID
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

        // Vérifier si l'utilisateur a les droits nécessaires (si c'est un admin)
        if (user.getRole().getName() != RoleType.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("L'utilisateur n'a pas les droits nécessaires.");
        }

        // Vérifier les champs obligatoires du produit
        if (produit.getNomProduit() == null || produit.getNomProduit().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "Le nom du produit est obligatoire."));
        }
        if (produit.getDescription() == null || produit.getDescription().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "La description du produit est obligatoire."));
        }
        if (produit.getPrix() == null || produit.getPrix() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "Le prix du produit doit être supérieur à 0."));
        }
        if (produit.getQuantite() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "La quantité doit être supérieure à 0."));
        }
        if (produit.getSeuil() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "Le seuil doit être supérieur à 0."));
        }
        if (produit.getAlertSeuil() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "L'alerte de seuil doit être un nombre positif."));
        }

        // Vérifier que la catégorie du produit existe
        if (produit.getCategory() == null || produit.getCategory().getId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "La catégorie du produit est obligatoire."));
        }

        // Récupérer la catégorie depuis la base de données
        CategoryProduit category = categoryProduitRepository.findById(produit.getCategory().getId())
                .orElseThrow(() -> new RuntimeException("La catégorie avec l'ID " + produit.getCategory().getId() + " n'existe pas."));

        // Associer la catégorie au produit
        produit.setCategory(category);

        // Sauvegarde du produit en base de données
        Produits savedProduit = produitsService.ajouterProduit(userId, produit);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduit);

    } catch (NotFoundException e) {
        // Log l'erreur NotFoundException
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
    } catch (Exception e) {
        // Log l'erreur générique
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
    }
}

        
        // Méthode pour Modifier un produit
            @PutMapping("/update/produit/{id}")
            public ResponseEntity<?> modifierProduit(
                    @RequestParam("userId") Long userId, // Ajout de l'ID de l'utilisateur
                    @PathVariable Long id,
                    @RequestParam("produit") String produitString,
                    @RequestParam(value = "image", required = false) MultipartFile imageFile
            ) {
                try {
                    // Vérifier les permissions de l'utilisateur avant de modifier le produit
                    User user = usersRepository.findById(userId)
                            .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));
                    authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);

                    // Convertir le produit string en objet
                    ObjectMapper objectMapper = new ObjectMapper();
                    objectMapper.registerModule(new JavaTimeModule());
                    Produits produitModifie = objectMapper.readValue(produitString, Produits.class);

                    // Appeler le service pour modifier le produit
                    Produits updatedProduit = produitsService.modifierProduit(userId, id, produitModifie, imageFile);

                    return ResponseEntity.ok(updatedProduit);
                } catch (NotFoundException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
                }
            }

            // Endpoint pour récupérer tous les produits
            @GetMapping("/list/produits")
            public ResponseEntity<?> getAllProduits() {
                try {
                    List<Produits> produits = produitsService.getAllProduits();
                    return ResponseEntity.ok(produits);
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
                }
            }

            // Endpoint pour récupérer un produit par son ID
            @GetMapping("/list/produit/{id}")
            public ResponseEntity<?> getProduitById(@PathVariable Long id) {
                try {
                    Produits produit = produitsService.getProduitById(id);
                    return ResponseEntity.ok(produit);
                } catch (NotFoundException e) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", e.getMessage()));
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
                }
            }

            // Endpoint pour supprimer un produit
            @DeleteMapping("/delete/produit/{id}")
            public ResponseEntity<?> deleteProduit(
            @RequestParam("userId") Long userId, // Ajout de l'ID de l'utilisateur
            @PathVariable Long id
    ) {
        try {
            // Vérifier les permissions de l'utilisateur avant de supprimer le produit
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));
            authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);

            // Appeler le service pour supprimer le produit
            String message = produitsService.deleteProduit(userId, id);

            return new ResponseEntity<>(message, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Erreur lors de la suppression du produit : " + e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
