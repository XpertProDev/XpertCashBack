package com.xpertcash.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Fournisseur;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.repository.FactureRepository;
import com.xpertcash.repository.FournisseurRepository;
import com.xpertcash.repository.StockProduitFournisseurRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.transaction.annotation.Transactional;






@Service
public class FournisseurService {
    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private StockProduitFournisseurRepository stockProduitFournisseurRepository;

     @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private FactureRepository factureRepository;

    

    @Autowired
    private AuthenticationHelper authHelper;

    // Save a new fournisseur
   public Fournisseur saveFournisseur(Fournisseur fournisseur, MultipartFile imageFournisseurFile, HttpServletRequest request) {
        if (fournisseur.getNomComplet() == null || fournisseur.getNomComplet().trim().isEmpty()) {
            throw new RuntimeException("Le nom du fournisseur est obligatoire !");
        }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entrepriseUtilisateur = user.getEntreprise();
    if (entrepriseUtilisateur == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    fournisseur.setEntreprise(entrepriseUtilisateur);
    fournisseur.setCreatedAt(java.time.LocalDateTime.now());


        checkFournisseurExists(fournisseur);

        if (imageFournisseurFile != null && !imageFournisseurFile.isEmpty()) {
            String imageUrl = imageStorageService.saveFournisseurImage(imageFournisseurFile);
            fournisseur.setPhoto(imageUrl);
            System.out.println(" Photo du fournisseur enregistrée : " + imageUrl);
        }

        return fournisseurRepository.save(fournisseur);
    }
   
    private void checkFournisseurExists(Fournisseur fournisseur) {
        String email = fournisseur.getEmail();
        String telephone = fournisseur.getTelephone();
        Long entrepriseId = fournisseur.getEntreprise() != null ? fournisseur.getEntreprise().getId() : null;

        if (entrepriseId == null) {
            throw new RuntimeException("Le fournisseur doit être associé à une entreprise.");
        }

        if (email != null && !email.isEmpty()) {
            fournisseurRepository.findByEmailAndEntrepriseId(email, entrepriseId).ifPresent(existing -> {
                throw new RuntimeException("Un fournisseur avec cet email existe déjà !");
            });
        }

        if (telephone != null && !telephone.isEmpty()) {
            fournisseurRepository.findByTelephoneAndEntrepriseId(telephone, entrepriseId).ifPresent(existing -> {
                throw new RuntimeException("Un fournisseur avec ce numéro de téléphone existe déjà !");
            });
        }
    }

    public List<Fournisseur> getFournisseursByEntreprise(HttpServletRequest request) {
        
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    return fournisseurRepository.findByEntrepriseId(entreprise.getId());
}

    // Get fournisseur by id
  public Fournisseur getFournisseurById(Long fournisseurId, HttpServletRequest request) {
    User user = getUserFromRequest(request);

    Entreprise entrepriseUtilisateur = user.getEntreprise();
    if (entrepriseUtilisateur == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    Fournisseur fournisseur = fournisseurRepository.findByIdAndEntrepriseId(
            fournisseurId, entrepriseUtilisateur.getId())
            .orElseThrow(() -> new RuntimeException("Fournisseur introuvable ou n'appartient pas à votre entreprise !"));

    return fournisseur;
}

private User getUserFromRequest(HttpServletRequest request) {
    return authHelper.getAuthenticatedUserWithFallback(request);
}


    // Update fournisseur
   public Fournisseur updateFournisseur(Long id, Fournisseur updatedData,MultipartFile imageFournisseurFile, HttpServletRequest request) {
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entrepriseUtilisateur = user.getEntreprise();
    if (entrepriseUtilisateur == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    Fournisseur existingFournisseur = fournisseurRepository.findByIdAndEntrepriseId(
            id, entrepriseUtilisateur.getId())
            .orElseThrow(() -> new RuntimeException("Fournisseur introuvable ou n'appartient pas à votre entreprise !"));



    if (updatedData.getEmail() != null && !updatedData.getEmail().isBlank()) {
        boolean emailExiste = fournisseurRepository.existsByEntrepriseIdAndEmailAndIdNot(
                entrepriseUtilisateur.getId(), updatedData.getEmail(), existingFournisseur.getId());
        if (emailExiste) {
            throw new RuntimeException("Un autre fournisseur avec cet email existe déjà dans votre entreprise.");
        }
    }

    if (updatedData.getTelephone() != null && !updatedData.getTelephone().isBlank()
            && updatedData.getPays() != null && !updatedData.getPays().isBlank()) {

        boolean telephoneExiste = fournisseurRepository.existsByEntrepriseIdAndPaysAndTelephoneAndIdNot(
                entrepriseUtilisateur.getId(), updatedData.getPays(), updatedData.getTelephone(), existingFournisseur.getId());

        if (telephoneExiste) {
            throw new RuntimeException("Un autre fournisseur avec ce numéro existe déjà dans ce pays au sein de votre entreprise.");
        }
    }

    existingFournisseur.setNomComplet(updatedData.getNomComplet());
    existingFournisseur.setNomSociete(updatedData.getNomSociete());
    existingFournisseur.setDescription(updatedData.getDescription());
    existingFournisseur.setPays(updatedData.getPays());
    existingFournisseur.setTelephone(updatedData.getTelephone());
    existingFournisseur.setEmail(updatedData.getEmail());
    existingFournisseur.setVille(updatedData.getVille());
    existingFournisseur.setAdresse(updatedData.getAdresse());

        if (imageFournisseurFile != null && !imageFournisseurFile.isEmpty()) {
            String oldImagePath = existingFournisseur.getPhoto();
            if (oldImagePath != null && !oldImagePath.isBlank()) {
                Path oldPath = Paths.get("src/main/resources/static" + oldImagePath);
                try {
                    Files.deleteIfExists(oldPath);
                    System.out.println(" Ancienne photo profil supprimée : " + oldImagePath);
                } catch (IOException e) {
                    System.out.println(" Impossible de supprimer l'ancienne photo : " + e.getMessage());
                }
            }

            String newImageUrl = imageStorageService.saveFournisseurImage(imageFournisseurFile);
            existingFournisseur.setPhoto(newImageUrl);
            System.out.println(" Nouvelle photo enregistrée : " + newImageUrl);
        }

    
    Fournisseur savedFournisseur = fournisseurRepository.save(existingFournisseur);
    return savedFournisseur;
}


  // Lister tout les stocks lieu a un fournisseur
    public List<Map<String, Object>> getNomProduitEtQuantiteAjoutee(Long fournisseurId,
    HttpServletRequest request) {
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Fournisseur fournisseur = fournisseurRepository.findByIdAndEntrepriseId(
            fournisseurId, user.getEntreprise().getId())
            .orElseThrow(() -> new RuntimeException("Fournisseur introuvable ou n'appartient pas à votre entreprise !"));


        List<Object[]> rows = stockProduitFournisseurRepository.findNomProduitEtQuantiteAjoutee(fournisseurId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("nomProduit", row[0]);
            map.put("quantiteAjoutee", row[1]);
            result.add(map);
        }
        return result;
    }
    

    // Supprimer un fournisseur
 @Transactional
  public void supprimerFournisseur(Long fournisseurId, HttpServletRequest request) {
    User user = getUserFromRequest(request);

    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;

    if (!isAdminOrManager) {
        throw new RuntimeException("Action non autorisée : permissions insuffisantes");
    }

    Fournisseur fournisseur = fournisseurRepository.findByIdAndEntrepriseId(
            fournisseurId, user.getEntreprise().getId())
        .orElseThrow(() -> new RuntimeException("Fournisseur introuvable ou n'appartient pas à votre entreprise !"));

    boolean fournisseurUtilise = factureRepository.existsByFournisseurIdAndEntrepriseId(
            fournisseurId, user.getEntreprise().getId());
    if (fournisseurUtilise) {
        throw new RuntimeException("Impossible de supprimer ce fournisseur : il est lié à une ou plusieurs factures.");
    }

    fournisseurRepository.delete(fournisseur);
}

}
