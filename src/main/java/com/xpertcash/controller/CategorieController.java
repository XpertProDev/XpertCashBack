package com.xpertcash.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.entity.Categorie;
import com.xpertcash.entity.Unite;
import com.xpertcash.repository.CategorieRepository;
import com.xpertcash.service.CategorieService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class CategorieController {
       @Autowired
    private CategorieService categorieService;
    @Autowired
    private CategorieRepository categorieRepository;

    // Ajouter une catégorie (seul ADMIN)
    @PostMapping("/createCategory")
    public ResponseEntity<Object> createCategorie(@RequestBody Map<String, String> payload) {
        try {
            String nom = payload.get("nom");

            if (nom == null || nom.isEmpty()) {
                // Si le nom de la catégorie est vide
                throw new RuntimeException("Catégorie ne peut pas être vide !");
            }

            // Vérifier si la catégorie existe déjà
            if (categorieRepository.existsByNom(nom)) {
                // Si la catégorie existe déjà, on renvoie une réponse 409 Conflict avec un message
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Cette catégorie existe déjà !");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // Créer la catégorie
            Categorie categorie = new Categorie();
            categorie.setNom(nom);
            categorie.setCreatedAt(LocalDateTime.now());

            // Sauvegarder la catégorie dans la base de données
            Categorie savedCategorie = categorieRepository.save(categorie);

            // Retourner la réponse avec la catégorie créée
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCategorie);
        } catch (RuntimeException e) {
            // Gestion des erreurs
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse); // Retour d'une erreur générique en cas de problème
        }
    }



        // Récupérer toutes les catégories
    @GetMapping("/allCategory")
    public ResponseEntity<List<Categorie>> getAllCategories() {
        return ResponseEntity.ok(categorieService.getAllCategories());
    }

    // Supprimer une catégorie
    @DeleteMapping("/deleteCategory/{id}")
    public ResponseEntity<String> deleteCategorie(@PathVariable Long id) {
        categorieService.deleteCategorie(id);
        return ResponseEntity.ok("Catégorie supprimée avec succès !");
    }

    //Update Categorie
    @PutMapping("/updateCategorie/{categorieId}")
    public ResponseEntity<Categorie> updateCategoriee(@PathVariable Long categorieId, @RequestBody Categorie categorieDetails, HttpServletRequest request) {
        try {
            // Appeler le service pour mettre à jour l'unité
            Categorie updateCategorie = categorieService.updateCategorie(request, categorieId, categorieDetails);
            return ResponseEntity.ok(updateCategorie);  
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

}
