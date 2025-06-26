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

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Fournisseur;
import com.xpertcash.entity.User;
import com.xpertcash.repository.FournisseurRepository;
import com.xpertcash.repository.StockProduitFournisseurRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;

import jakarta.servlet.http.HttpServletRequest;





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
    private JwtUtil jwtUtil;

    // Save a new fournisseur
   public Fournisseur saveFournisseur(Fournisseur fournisseur, MultipartFile imageFournisseurFile, HttpServletRequest request) {
        if (fournisseur.getNomComplet() == null || fournisseur.getNomComplet().trim().isEmpty()) {
            throw new RuntimeException("Le nom du fournisseur est obligatoire !");
        }

          // Vérifier la présence du token JWT et récupérer l'ID de l'utilisateur connecté
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId = null;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'utilisateur depuis le token", e);
    }

    // Récupérer l'utilisateur par son ID
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

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
        
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId;
    try {
        token = token.replace("Bearer ", "");
        userId = jwtUtil.extractUserId(token);
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token", e);
    }

    // Récupérer l'utilisateur
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

    // Vérifier la présence d'une entreprise liée à l'utilisateur
    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    // Retourner tous les fournisseurs liés à cette entreprise
    return fournisseurRepository.findByEntreprise(entreprise);
}

    // Get fournisseur by id
    public Fournisseur getFournisseurById(Long id, HttpServletRequest request) {

    // Vérifier la présence du token JWT et récupérer l'ID de l'utilisateur connecté
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'utilisateur depuis le token", e);
    }

    // Récupérer l'utilisateur par son ID
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

    // Vérifier que l'utilisateur a une entreprise associée
    Entreprise entrepriseUtilisateur = user.getEntreprise();
    if (entrepriseUtilisateur == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    // Récupérer le fournisseur
    Fournisseur fournisseur = fournisseurRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Fournisseur introuvable !"));

    // Vérifier que le fournisseur appartient à la même entreprise que l'utilisateur
    if (fournisseur.getEntreprise() == null || 
        !fournisseur.getEntreprise().getId().equals(entrepriseUtilisateur.getId())) {
        throw new RuntimeException("Ce fournisseur n'appartient pas à votre entreprise.");
    }

    return fournisseur;
}


    // Update fournisseur
   public Fournisseur updateFournisseur(Long id, Fournisseur updatedData,MultipartFile imageFournisseurFile, HttpServletRequest request) {
    // 1. Extraire le token JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'utilisateur depuis le token", e);
    }

    // 2. Vérifier l'utilisateur et son entreprise
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

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

    // 4. Mettre à jour les champs
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

            String newImageUrl = imageStorageService.saveClientImage(imageFournisseurFile);
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
          String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }
      Long userId;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'utilisateur depuis le token", e);
    }
     User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

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
    
}
