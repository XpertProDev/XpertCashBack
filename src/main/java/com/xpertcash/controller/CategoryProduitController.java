package com.xpertcash.controller;

import com.xpertcash.entity.CategoryProduit;
import com.xpertcash.service.CategoryProduitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class CategoryProduitController {

    @Autowired
    private CategoryProduitService categoryProduitService;

    // Créer une catégorie.
    @PostMapping("/add/categoryProduit")
    public ResponseEntity<?> createCategory(@RequestBody CategoryProduit category) {
        try {
            CategoryProduit createdCategory = categoryProduitService.createCategory(category);
            return ResponseEntity.ok(createdCategory);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la création de la catégorie : " + e.getMessage());
        }
    }

    // Mettre à jour une catégorie.
    @PutMapping("/update/categoryProduit/{id}")
    public ResponseEntity<String> updateCategory(@PathVariable Long id, @RequestBody CategoryProduit category) {
        try {
            categoryProduitService.updateCategory(id, category);
            return ResponseEntity.ok("Catégorie mise à jour avec succès");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la mise à jour : " + e.getMessage());
        }
    }

    // Supprimer une catégorie.
    @DeleteMapping("/delete/categoryProduit/{id}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long id) {
        try {
            categoryProduitService.deleteCategory(id);
            return ResponseEntity.ok("Catégorie supprimée avec succès");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la suppression : " + e.getMessage());
        }
    }

    // Lister toutes les catégories.
    @GetMapping("/list/categoryProduit")
    public ResponseEntity<?> getAllCategories() {
        try {
            List<CategoryProduit> categories = categoryProduitService.getAllCategories();
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la récupération des catégories : " + e.getMessage());
        }
    }

    // Récupérer une catégorie par son ID et produit.
    @GetMapping("/list/categoryProduit/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        try {
            CategoryProduit category = categoryProduitService.getCategoryById(id);
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la recherche de la catégorie : " + e.getMessage());
        }
    }


}
