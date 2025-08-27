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

import com.xpertcash.DTOs.CategorieResponseDTO;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Categorie;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Unite;
import com.xpertcash.entity.User;
import com.xpertcash.repository.CategorieRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.CategorieService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class CategorieController {
       @Autowired
    private CategorieService categorieService;
    @Autowired
    private CategorieRepository categorieRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private JwtUtil jwtUtil;

    // Ajouter une catégorie (seul ADMIN)
    @PostMapping("/createCategory")
public ResponseEntity<Object> createCategorie(@RequestBody Map<String, String> payload, HttpServletRequest request) {
    try {
        String nom = payload.get("nom");
        
        // Récupérer l'ID de l'entreprise de l'utilisateur authentifié à partir du token JWT
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long userId;
        try {
            userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur", e);
        }

        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        // Si le nom de la catégorie est vide
        if (nom == null || nom.isEmpty()) {
            throw new RuntimeException("Catégorie ne peut pas être vide !");
        }

        // Vérifier si la catégorie existe déjà
        if (categorieRepository.existsByNom(nom)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Cette catégorie existe déjà !");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        // Créer la catégorie et l'associer à l'entreprise de l'utilisateur
        Categorie savedCategorie = categorieService.createCategorie(nom, entreprise.getId());

        // Retourner un message de succès ou un objet simple
        Map<String, Object> response = new HashMap<>();
        response.put("id", savedCategorie.getId());
        response.put("nom", savedCategorie.getNom());
        response.put("message", "Catégorie créée avec succès !");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    } catch (RuntimeException e) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
}


        // Récupérer toutes les catégories
        @GetMapping("/allCategory")
        public ResponseEntity<List<CategorieResponseDTO>> getAllCategories(HttpServletRequest request) {
            try {
                List<CategorieResponseDTO> categoriesAvecProduitCount = categorieService.getCategoriesWithProduitCount(request);

                return ResponseEntity.ok(categoriesAvecProduitCount);
                
            } catch (RuntimeException e) {
                System.err.println("Erreur lors de la récupération des catégories : " + e.getMessage());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
                
            } catch (Exception e) {
                System.err.println("Erreur interne lors de la récupération des catégories : " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }

    

    // Supprimer une catégorie
    @DeleteMapping("/deleteCategory/{id}")
    public ResponseEntity<?> supprimerCategorieSiVide(
            @PathVariable("id") Long categorieId,
            HttpServletRequest request) {
        try {
            categorieService.supprimerCategorieSiVide(categorieId, request);
            return ResponseEntity.ok("Catégorie supprimée avec succès.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la suppression.");
        }
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
