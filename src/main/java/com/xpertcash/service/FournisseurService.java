package com.xpertcash.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Fournisseur;
import com.xpertcash.entity.User;
import com.xpertcash.repository.FournisseurRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class FournisseurService {
    @Autowired
    private FournisseurRepository fournisseurRepository;
    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // Save a new fournisseur
    public Fournisseur saveFournisseur(Fournisseur fournisseur, HttpServletRequest request) {
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

        return fournisseurRepository.findByEntreprise(entrepriseUtilisateur);
    }

  

}
