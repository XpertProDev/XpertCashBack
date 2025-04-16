package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.Categorie;
import com.xpertcash.repository.CategorieRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class CategorieService {
    @Autowired
    private CategorieRepository categorieRepository;

     // Ajouter une nouvelle catégorie (seul ADMIN peut le faire)
    public Categorie createCategorie(String nom) {
        if (categorieRepository.existsByNom(nom)) {
            throw new RuntimeException("Cette catégorie existe déjà !");
        }

        Categorie categorie = new Categorie();
        categorie.setNom(nom);
        categorie.setCreatedAt(LocalDateTime.now());
        return categorieRepository.save(categorie);
    }

    // Récupérer toutes les catégories
    public List<Categorie> getAllCategories() {
        return categorieRepository.findAll();
    }

     // Supprimer une catégorie
     public void deleteCategorie(Long id) {
        categorieRepository.deleteById(id);
    }

    // Mettre à jour categorie
    public Categorie updateCategorie(HttpServletRequest request, Long categorieId, Categorie categorieDetails) {
        try {
            Categorie categorie = categorieRepository.findById(categorieId)
                    .orElseThrow(() -> new RuntimeException("Categorie non trouvée"));

                    if (categorieRepository.existsByNom(categorieDetails.getNom())) {
                        throw new RuntimeException("Le nom cette categorie existe déjà.");
                    }
            
    
            
            categorie.setNom(categorieDetails.getNom());
    
            // Enregistrer l'unité mise à jour
            return categorieRepository.save(categorie);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour du categorie : " + e.getMessage());
        }
    }

}
