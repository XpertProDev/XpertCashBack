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
                // Extraire id de user à partir de son token
                String jwtToken = token.substring(7);
                Long userId = jwtUtil.extractUserId(jwtToken); 

                User user = usersRepository.findById(userId)
                        .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé"));

                // Vérifier si l'utilisateur est un admin
                if (user.getRole().getName() != RoleType.ADMIN) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body("Vous n'avez pas les droits nécessaires.");
                }

                // Récupérer l'entreprise de l'utilisateur
                Entreprise entreprise = user.getEntreprise();
                if (entreprise == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("L'administrateur n'appartient à aucune entreprise.");
                }
                category.setEntreprise(entreprise);

                // Créer la catégorie
                CategoryProduit createdCategory = categoryProduitService.createCategory(category);
                return ResponseEntity.status(HttpStatus.CREATED).body(createdCategory);

            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Erreur lors de la création de la catégorie : " + e.getMessage());
            }
        }

}
