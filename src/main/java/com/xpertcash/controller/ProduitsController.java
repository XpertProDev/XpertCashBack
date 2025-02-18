package com.xpertcash.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Produits;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;
import com.xpertcash.service.ProduitsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    private AuthorizationService authorizationService;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private ImageStorageService imageStorageService;
    @Autowired
    private JwtUtil jwtUtil;

    
        // Méthode pour Ajouter un produit
        @PostMapping(value = "/add/produit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<?> ajouterProduit(
                @RequestHeader("Authorization") String token,
                @RequestPart("produit") String produitJson,
                @RequestPart("photo") MultipartFile photo) {
            try {
                // Convertir le JSON en objet Produits
                ObjectMapper mapper = new ObjectMapper();
                Produits produit = mapper.readValue(produitJson, Produits.class);

                // Extraire l'ID de l'utilisateur à partir du token
                String jwtToken = token.substring(7);
                Long userId = jwtUtil.extractUserId(jwtToken);

                // Vérifier si l'utilisateur existe
                User user = usersRepository.findById(userId)
                        .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

                // Vérifier si l'utilisateur est admin
                if (!user.getRole().getName().equals(RoleType.ADMIN)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Collections.singletonMap("error", "Vous n'avez pas les droits nécessaires."));
                }

                // Vérification des permissions
                authorizationService.checkPermission(user, PermissionType.GERER_PRODUITS);

                // Sauvegarder l'image via le service et récupérer son URL
                String imageUrl = imageStorageService.saveImage(photo);
                // Associer l'URL de l'image au produit
                produit.setPhoto(imageUrl);

                // Ajouter le produit via le service
                Produits savedProduit = produitsService.ajouterProduit(userId, produit);

                return ResponseEntity.status(HttpStatus.CREATED).body(savedProduit);

            } catch (NotFoundException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", e.getMessage()));
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("error", e.getMessage()));
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
