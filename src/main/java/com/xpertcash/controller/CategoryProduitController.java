package com.xpertcash.controller;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.CategoryProduit;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.CategoryProduitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class CategoryProduitController {

    @Autowired
    private CategoryProduitService categoryProduitService;
    @Autowired
    private UsersRepository usersRepository;
     @Autowired
    private JwtUtil jwtUtil;


        // Créer une catégorie.
    @PostMapping("/add/categoryProduit")
    public ResponseEntity<?> createCategory(@RequestBody CategoryProduit category, @RequestHeader("Authorization") String token) {
        try {
            // Extraire l'ID de l'utilisateur à partir du token JWT
            String jwtToken = token.substring(7); // Enlever le "Bearer " du début du token
            Long userId = jwtUtil.extractUserId(jwtToken);  // Extraire l'ID de l'utilisateur

            // Récupérer l'utilisateur via son ID
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

            // Vérifier si l'utilisateur est un admin
            if (user.getRole().getName() != RoleType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("L'utilisateur n'a pas les droits nécessaires.");
            }

            // Récupérer l'entreprise de l'utilisateur
            Entreprise entreprise = user.getEntreprise();
            if (entreprise == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("L'administrateur n'appartient à aucune entreprise.");
            }

            // Associer l'entreprise à la catégorie
            category.setEntreprise(entreprise);

            // Créer la catégorie
            CategoryProduit createdCategory = categoryProduitService.createCategory(category);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la création de la catégorie : " + e.getMessage());
        }
    }

    // Mettre à jour une catégorie.
    @PutMapping("/update/categoryProduit/{id}")
    public ResponseEntity<String> updateCategory(@PathVariable Long id, @RequestBody CategoryProduit category, @RequestHeader("Authorization") String token) {
        try {
            // Extraire l'ID de l'utilisateur à partir du token JWT
            String jwtToken = token.substring(7); // Enlever le "Bearer " du début du token
            Long userId = jwtUtil.extractUserId(jwtToken);  // Extraire l'ID de l'utilisateur

            // Récupérer l'utilisateur via son ID
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

            // Vérifier si l'utilisateur est un admin
            if (user.getRole().getName() != RoleType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("L'utilisateur n'a pas les droits nécessaires.");
            }

            // Mettre à jour la catégorie
            categoryProduitService.updateCategory(id, category);
            return ResponseEntity.ok("Catégorie mise à jour avec succès");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la mise à jour : " + e.getMessage());
        }
    }

    // Supprimer une catégorie.
    @DeleteMapping("/delete/categoryProduit/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            // Extraire l'ID de l'utilisateur à partir du token JWT
            String jwtToken = token.substring(7); // Enlever le "Bearer " du début du token
            Long userId = jwtUtil.extractUserId(jwtToken);  // Extraire l'ID de l'utilisateur

            // Récupérer l'utilisateur via son ID
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

            // Vérifier si l'utilisateur est un admin
            if (user.getRole().getName() != RoleType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("L'utilisateur n'a pas les droits nécessaires.");
            }

            // Supprimer la catégorie
            categoryProduitService.deleteCategory(id);
            return ResponseEntity.ok("Catégorie supprimée avec succès");

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la suppression : " + e.getMessage());
        }
    }

    // Lister toutes les catégories.
    @GetMapping("/list/categoryProduit")
    public ResponseEntity<?> getAllCategories(@RequestHeader("Authorization") String token) {
        try {
            // Extraire l'ID de l'utilisateur à partir du token JWT
            String jwtToken = token.substring(7); // Enlever le "Bearer " du début du token
            Long userId = jwtUtil.extractUserId(jwtToken);  // Extraire l'ID de l'utilisateur

            // Récupérer l'utilisateur via son ID
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

            // Vérifier si l'utilisateur est un admin
            if (user.getRole().getName() != RoleType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("L'utilisateur n'a pas les droits nécessaires.");
            }

            // Lister toutes les catégories
            List<CategoryProduit> categories = categoryProduitService.getAllCategories();
            return ResponseEntity.ok(categories);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération des catégories : " + e.getMessage());
        }
    }

    // Récupérer une catégorie par son ID et produit.
    @GetMapping("/list/categoryProduit/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        try {
            // Extraire l'ID de l'utilisateur à partir du token JWT
            String jwtToken = token.substring(7); // Enlever le "Bearer " du début du token
            Long userId = jwtUtil.extractUserId(jwtToken);  // Extraire l'ID de l'utilisateur

            // Récupérer l'utilisateur via son ID
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

            // Vérifier si l'utilisateur est un admin
            if (user.getRole().getName() != RoleType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("L'utilisateur n'a pas les droits nécessaires.");
            }

            // Récupérer la catégorie par son ID
            CategoryProduit category = categoryProduitService.getCategoryById(id);
            return ResponseEntity.ok(category);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la recherche de la catégorie : " + e.getMessage());
        }
    }


}
