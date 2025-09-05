package com.xpertcash.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Fournisseur;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.repository.FactureRepository;
import com.xpertcash.repository.FournisseurRepository;
import com.xpertcash.repository.StockProduitFournisseurRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;
import com.xpertcash.service.AuthenticationHelper;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.transaction.annotation.Transactional;






@Service
public class FournisseurService {
    @Autowired
    private FournisseurRepository fournisseurRepository;
    @Autowired
    private UsersRepository usersRepository;

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

    // Vérifier que l'utilisateur a une entreprise associée (entreprise créatrice de la facture)
    Entreprise entrepriseUtilisateur = user.getEntreprise();
    if (entrepriseUtilisateur == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    fournisseur.setEntreprise(entrepriseUtilisateur);
    fournisseur.setCreatedAt(java.time.LocalDateTime.now());


        checkFournisseurExists(fournisseur);

        // Gestion de l'image si présente
        if (imageFournisseurFile != null && !imageFournisseurFile.isEmpty()) {
            String imageUrl = imageStorageService.saveFournisseurImage(imageFournisseurFile);
            fournisseur.setPhoto(imageUrl);
            System.out.println("📸 Photo du fournisseur enregistrée : " + imageUrl);
        }

        return fournisseurRepository.save(fournisseur);
    }
   
    private void checkFournisseurExists(Fournisseur fournisseur) {
        String email = fournisseur.getEmail();
        String telephone = fournisseur.getTelephone();

        if (email != null && !email.isEmpty()) {
            fournisseurRepository.findByEmail(email).ifPresent(existing -> {
                throw new RuntimeException("Un fournisseur avec cet email existe déjà !");
            });
        }

        if (telephone != null && !telephone.isEmpty()) {
            fournisseurRepository.findByTelephone(telephone).ifPresent(existing -> {
                throw new RuntimeException("Un fournisseur avec ce numéro de téléphone existe déjà !");
            });
        }
    }

    // Get fournisseur dune entreprise de l utilisateur 
    public List<Fournisseur> getFournisseursByEntreprise(HttpServletRequest request) {
        
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    // Vérifier la présence d'une entreprise liée à l'utilisateur
    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    // Retourner tous les fournisseurs liés à cette entreprise
    return fournisseurRepository.findByEntreprise(entreprise);
}

    // Get fournisseur by id
  public Fournisseur getFournisseurById(Long fournisseurId, HttpServletRequest request) {
    User user = getUserFromRequest(request);

    // Vérifier que l'utilisateur a une entreprise associée
    Entreprise entrepriseUtilisateur = user.getEntreprise();
    if (entrepriseUtilisateur == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    // Récupérer le fournisseur
    Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
            .orElseThrow(() -> new RuntimeException("Fournisseur introuvable !"));

    // Vérifier que le fournisseur appartient à la même entreprise que l'utilisateur
    if (fournisseur.getEntreprise() == null ||
        !fournisseur.getEntreprise().getId().equals(entrepriseUtilisateur.getId())) {
        throw new RuntimeException("Ce fournisseur n'appartient pas à votre entreprise.");
    }

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

    // 3. Récupérer le fournisseur existant
    Fournisseur existingFournisseur = fournisseurRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Fournisseur introuvable !"));

    if (existingFournisseur.getEntreprise() == null || 
        !existingFournisseur.getEntreprise().getId().equals(entrepriseUtilisateur.getId())) {
        throw new RuntimeException("Ce fournisseur n'appartient pas à votre entreprise.");
    }


    // 4. Vérifications des doublons

    // Vérifier email unique dans l'entreprise (hors du fournisseur qu'on modifie)
    if (updatedData.getEmail() != null && !updatedData.getEmail().isBlank()) {
        boolean emailExiste = fournisseurRepository.existsByEntrepriseIdAndEmailAndIdNot(
                entrepriseUtilisateur.getId(), updatedData.getEmail(), existingFournisseur.getId());
        if (emailExiste) {
            throw new RuntimeException("Un autre fournisseur avec cet email existe déjà dans votre entreprise.");
        }
    }

        // Vérifier téléphone unique par pays dans l'entreprise (hors du fournisseur qu'on modifie)
    if (updatedData.getTelephone() != null && !updatedData.getTelephone().isBlank()
            && updatedData.getPays() != null && !updatedData.getPays().isBlank()) {

        boolean telephoneExiste = fournisseurRepository.existsByEntrepriseIdAndPaysAndTelephoneAndIdNot(
                entrepriseUtilisateur.getId(), updatedData.getPays(), updatedData.getTelephone(), existingFournisseur.getId());

        if (telephoneExiste) {
            throw new RuntimeException("Un autre fournisseur avec ce numéro existe déjà dans ce pays au sein de votre entreprise.");
        }
    }

    // 5. Mettre à jour les champs
    existingFournisseur.setNomComplet(updatedData.getNomComplet());
    existingFournisseur.setNomSociete(updatedData.getNomSociete());
    existingFournisseur.setDescription(updatedData.getDescription());
    existingFournisseur.setPays(updatedData.getPays());
    existingFournisseur.setTelephone(updatedData.getTelephone());
    existingFournisseur.setEmail(updatedData.getEmail());
    existingFournisseur.setVille(updatedData.getVille());
    existingFournisseur.setAdresse(updatedData.getAdresse());

     // Mise à jour de la photo si image présente
        if (imageFournisseurFile != null && !imageFournisseurFile.isEmpty()) {
            String oldImagePath = existingFournisseur.getPhoto(); // ✅ Prendre depuis l'objet actuel en base
            if (oldImagePath != null && !oldImagePath.isBlank()) {
                Path oldPath = Paths.get("src/main/resources/static" + oldImagePath);
                try {
                    Files.deleteIfExists(oldPath);
                    System.out.println("🗑️ Ancienne photo profil supprimée : " + oldImagePath);
                } catch (IOException e) {
                    System.out.println("⚠️ Impossible de supprimer l'ancienne photo : " + e.getMessage());
                }
            }

            String newImageUrl = imageStorageService.saveFournisseurImage(imageFournisseurFile);
            existingFournisseur.setPhoto(newImageUrl);
            System.out.println("📸 Nouvelle photo enregistrée : " + newImageUrl);
        }

    
    // Enregistrer les modifications et retourner l'entité mise à jour
    Fournisseur savedFournisseur = fournisseurRepository.save(existingFournisseur);
    return savedFournisseur;
}


  // Lister tout les stocks lieu a un fournisseur
    public List<Map<String, Object>> getNomProduitEtQuantiteAjoutee(Long fournisseurId,
    HttpServletRequest request) {
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    // 2. Vérifier que le fournisseur appartient à la même entreprise
    Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
            .orElseThrow(() -> new RuntimeException("Fournisseur introuvable !"));

    if (!fournisseur.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès refusé : Ce fournisseur n'appartient pas à votre entreprise !");
    }


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

    // 🔒 Vérification rôle ou permission
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;

    if (!isAdminOrManager) {
        throw new RuntimeException("Action non autorisée : permissions insuffisantes");
    }

    // 🔍 Récupération fournisseur
    Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
        .orElseThrow(() -> new RuntimeException("Fournisseur introuvable !"));

    // 🏢 Vérification entreprise
    if (fournisseur.getEntreprise() == null || 
        !fournisseur.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Ce fournisseur n'appartient pas à votre entreprise.");
    }

    // 📄 Vérification d'utilisation dans facture
    boolean fournisseurUtilise = factureRepository.existsByFournisseur_Id(fournisseurId);
    if (fournisseurUtilise) {
        throw new RuntimeException("Impossible de supprimer ce fournisseur : il est lié à une ou plusieurs factures.");
    }

    // ✅ Suppression
    fournisseurRepository.delete(fournisseur);
}

}
