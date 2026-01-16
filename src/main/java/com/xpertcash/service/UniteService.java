package com.xpertcash.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    @Transactional
    public Unite createUnite(Unite unite, HttpServletRequest request) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            Entreprise entreprise = user.getEntreprise();
            
            if (entreprise == null) {
                throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
            }
            
            if (unite.getNom() == null || unite.getNom().trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom de l'unité ne peut pas être vide.");
            }

            if (uniteRepository.existsByNomAndEntrepriseId(unite.getNom(), entreprise.getId())) {
                throw new IllegalArgumentException("Cette unité de mesure existe déjà.");
            }

            unite.setEntreprise(entreprise);

            return uniteRepository.save(unite);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de l'unité : " + e.getMessage());
        }
    }
    


    //Récupérer toutes les unités de mesure de l'entreprise de l'utilisateur connecté
    public List<Unite> getAllUnites(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }
        
        return uniteRepository.findByEntrepriseId(entreprise.getId());
    }

    //Récupérer unité de mesure par son ID (vérifie qu'elle appartient à l'entreprise de l'utilisateur)
    public Unite getUniteById(Long id, HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        Entreprise entreprise = user.getEntreprise();
        
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }
        
        return uniteRepository.findByIdAndEntrepriseId(id, entreprise.getId())
                .orElseThrow(() -> new RuntimeException("Unité de mesure non trouvée ou n'appartient pas à votre entreprise"));
    }



    // Mettre à jour l'unité
    @Transactional
    public Unite updateUnite(HttpServletRequest request, Long uniteId, Unite uniteDetails) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            Entreprise entreprise = user.getEntreprise();
            
            if (entreprise == null) {
                throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
            }
            
            Unite unite = uniteRepository.findByIdAndEntrepriseId(uniteId, entreprise.getId())
                    .orElseThrow(() -> new RuntimeException("Unité non trouvée ou n'appartient pas à votre entreprise"));

            if (!unite.getNom().equals(uniteDetails.getNom())) {
                Optional<Unite> uniteAvecMemeNom = uniteRepository.findByNomAndEntrepriseId(uniteDetails.getNom(), entreprise.getId());
                if (uniteAvecMemeNom.isPresent() && !uniteAvecMemeNom.get().getId().equals(uniteId)) {
                    throw new RuntimeException("Le nom de l'unité existe déjà.");
                }
            }
    
            
            unite.setNom(uniteDetails.getNom());
    
            return uniteRepository.save(unite);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour de l'unité : " + e.getMessage());
        }
    }
  

}
