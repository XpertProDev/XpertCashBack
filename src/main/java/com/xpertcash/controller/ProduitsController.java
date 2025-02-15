package com.xpertcash.controller;
import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produits;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.ProduitsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/auth")


public class ProduitsController {

    @Autowired
    private ProduitsService produitsService;

    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private JwtUtil jwtUtil;

    
        // Méthode pour Ajouter un produit
        @PostMapping("/add/produit")
        public ResponseEntity<?> ajouterProduit(
                @RequestHeader("Authorization") String token, 
                @RequestBody Produits produit ) {
            try {
                // Extraire id de l'utilisateur à partir du token
                String jwtToken = token.substring(7);  
                Long userId = jwtUtil.extractUserId(jwtToken); 

                User user = usersRepository.findById(userId)
                        .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

                // Vérifier si l'utilisateur a les droits nécessaires (si c'est un admin)
                if (user.getRole().getName() != RoleType.ADMIN) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Vous n'avez pas les droits nécessaires.");
                }
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

                    Produits savedProduit = produitsService.ajouterProduit(userId, produit);

                    return ResponseEntity.status(HttpStatus.CREATED).body(savedProduit);

                } catch (RuntimeException e) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
                }
        }


          // Endpoint pour récupérer les produits de l'entreprise de l'utilisateur
          @GetMapping("/entreprise/produits")
          public ResponseEntity<Object> listerProduitsEntreprise(@RequestHeader("Authorization") String token) {
              String jwtToken = token.substring(7); 
              Long userId = jwtUtil.extractUserId(jwtToken);

              try {
                  List<Produits> produits = produitsService.listerProduitsEntreprise(userId);
                  return ResponseEntity.ok(produits);
          
              } catch (RuntimeException e) {
                  return ResponseEntity.status(HttpStatus.FORBIDDEN)
                          .body(Collections.singletonMap("error", e.getMessage()));
              } catch (Exception e) {
                  return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                          .body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
              }
          }
          
}
