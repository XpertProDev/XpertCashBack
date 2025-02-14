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
        @PostMapping("/add/produit")
        public ResponseEntity<?> ajouterProduit(
                @RequestHeader("Authorization") String token,  // Récupération du token JWT
                @RequestBody Produits produit ) {
            try {
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

                // Vérifier si l'Admin a bien la permission de créer un produit
                authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);

                // Validation des champs du produit
                if (produit.getNomProduit() == null || produit.getNomProduit().trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Le nom du produit est obligatoire.");
                }
                if (produit.getDescription() == null || produit.getDescription().trim().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("La description du produit est obligatoire.");
                }
                if (produit.getPrix() == null || produit.getPrix() <= 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Le prix du produit doit être supérieur à 0.");
                }
                if (produit.getPrixAchat() == null || produit.getPrixAchat() <= 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Collections.singletonMap("error", "Le prix d'achat du produit est obligatoire et doit être supérieur à 0."));
                }
                if (produit.getQuantite() <= 0) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("La quantité doit être supérieure à 0.");
                }
                

                // Appeler le service pour ajouter le produit
                Produits savedProduit = produitsService.ajouterProduit(userId, produit);

                return ResponseEntity.status(HttpStatus.CREATED).body(savedProduit);

            } catch (RuntimeException e) {
                // En cas d'erreur (par exemple si l'utilisateur n'a pas les droits ou si les champs sont invalides)
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
            } catch (Exception e) {
                // Erreur générique
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
            }
        }


}
