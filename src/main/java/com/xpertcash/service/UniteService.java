package com.xpertcash.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Unite;
import com.xpertcash.entity.User;
import com.xpertcash.repository.UniteRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class UniteService {

   @Autowired
   private UniteRepository uniteRepository;
   
   @Autowired
   private AuthenticationHelper authHelper;


    // Ajouter une nouvelle unité
    public Unite createUnite(Unite unite, HttpServletRequest request) {
        try {
            // Récupérer l'utilisateur authentifié
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            Entreprise entreprise = user.getEntreprise();
            
            if (entreprise == null) {
                throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
            }
            
            // Vérifier que le nom de l'unité est valide
            if (unite.getNom() == null || unite.getNom().trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom de l'unité ne peut pas être vide.");
            }

            // Vérifier si l'unité existe déjà pour cette entreprise
            if (uniteRepository.existsByNomAndEntrepriseId(unite.getNom(), entreprise.getId())) {
                throw new IllegalArgumentException("Cette unité de mesure existe déjà.");
            }

            // Associer l'unité à l'entreprise de l'utilisateur
            unite.setEntreprise(entreprise);

            // Enregistrer l'unité dans la base de données
            return uniteRepository.save(unite);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de l'unité : " + e.getMessage());
        }
    }
    


    //Récupérer toutes les unités de mesure de l'entreprise de l'utilisateur connecté
    public List<Unite> getAllUnites(HttpServletRequest request) {
        // Récupérer l'utilisateur authentifié
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }
        
        // Retourner uniquement les unités de l'entreprise de l'utilisateur
        return uniteRepository.findByEntrepriseId(entreprise.getId());
    }

    //Récupérer unité de mesure par son ID (vérifie qu'elle appartient à l'entreprise de l'utilisateur)
    public Unite getUniteById(Long id, HttpServletRequest request) {
        // Récupérer l'utilisateur authentifié
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }
        
        // Vérifier que l'unité appartient à l'entreprise de l'utilisateur
        return uniteRepository.findByIdAndEntrepriseId(id, entreprise.getId())
                .orElseThrow(() -> new RuntimeException("Unité de mesure non trouvée ou n'appartient pas à votre entreprise"));
    }



    // Mettre à jour l'unité
    public Unite updateUnite(HttpServletRequest request, Long uniteId, Unite uniteDetails) {
        try {
            // Récupérer l'utilisateur authentifié
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            Entreprise entreprise = user.getEntreprise();
            
            if (entreprise == null) {
                throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
            }
            
            // Vérifier que l'unité existe et appartient à l'entreprise de l'utilisateur
            Unite unite = uniteRepository.findByIdAndEntrepriseId(uniteId, entreprise.getId())
                    .orElseThrow(() -> new RuntimeException("Unité non trouvée ou n'appartient pas à votre entreprise"));

            // Vérifier si le nom de l'unité est déjà utilisé par une autre unité de la même entreprise
            if (uniteRepository.existsByNomAndEntrepriseId(uniteDetails.getNom(), entreprise.getId())) {
                // Vérifier que ce n'est pas la même unité (on peut garder le même nom pour la même unité)
                Unite uniteAvecMemeNom = uniteRepository.findByNomAndEntrepriseId(uniteDetails.getNom(), entreprise.getId())
                        .orElse(null);
                if (uniteAvecMemeNom != null && !uniteAvecMemeNom.getId().equals(uniteId)) {
                    throw new RuntimeException("Le nom de l'unité existe déjà.");
                }
            }
    
            
            unite.setNom(uniteDetails.getNom());
    
            // Enregistrer l'unité mise à jour
            return uniteRepository.save(unite);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour de l'unité : " + e.getMessage());
        }
    }
  

}
