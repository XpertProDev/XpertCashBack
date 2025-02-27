package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.Categorie;
import com.xpertcash.entity.Unite;
import com.xpertcash.entity.User;
import com.xpertcash.repository.UniteRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class UniteService {

   @Autowired
   private UniteRepository uniteRepository;


    // Ajouter une nouvelle unité
    public Unite createUnite(Unite unite) {
        try {
            // Vérifier que le nom de l'unité est valide
            if (unite.getNom() == null || unite.getNom().trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom de l'unité ne peut pas être vide.");
            }

            // Vérifier si l'unité existe déjà
            if (uniteRepository.existsByNom(unite.getNom())) {
                throw new IllegalArgumentException("Cette unité de mesure existe déjà.");
            }

            // Enregistrer l'unité dans la base de données
            return uniteRepository.save(unite);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de l'unité : " + e.getMessage());
        }
    }
    


    //Récupérer toutes les unités de mesure
    public List<Unite> getAllUnites() {
        return uniteRepository.findAll();
    }

    //Récupérer unité de mesure par son ID
    public Unite getUniteById(Long id) {
        return uniteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Unité de mesure non trouvée"));
    }



    // Mettre à jour l'unité
    public Unite updateUnite(HttpServletRequest request, Long uniteId, Unite uniteDetails) {
        try {
            Unite unite = uniteRepository.findById(uniteId)
                    .orElseThrow(() -> new RuntimeException("Unité non trouvée"));

            // Vérifier si le nom de l'unité est déjà utilisé
            if (uniteRepository.existsByNom(uniteDetails.getNom())) {
                throw new RuntimeException("Le nom de l'unité existe déjà.");
            }
    
            
            unite.setNom(uniteDetails.getNom());
    
            // Enregistrer l'unité mise à jour
            return uniteRepository.save(unite);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour de l'unité : " + e.getMessage());
        }
    }
  

}
