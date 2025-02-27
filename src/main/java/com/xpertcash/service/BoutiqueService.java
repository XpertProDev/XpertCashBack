package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.User;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class BoutiqueService {

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private JwtUtil jwtUtil; 

    @Autowired
    private UsersRepository usersRepository;

    // Ajouter une nouvelle boutique pour l'admin
    @Transactional
    public Boutique ajouterBoutique(HttpServletRequest request, String nomBoutique, String adresse) {
        // Vérifier la présence du token JWT dans l'entête de la requête
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        // Extraire l'ID de l'admin depuis le token
        Long adminId = null;
        try {
            adminId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'admin depuis le token", e);
        }
           // Récupérer l'admin par son ID
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        // Vérifier que l'admin est bien un Admin
        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un admin peut ajouter une boutique !");
        }

        // Vérifier que l'admin possède une entreprise
        if (admin.getEntreprise() == null) {
            throw new RuntimeException("L'Admin n'a pas d'entreprise associée.");
        }

        // Créer une nouvelle boutique pour l'entreprise de l'admin
        Boutique boutique = new Boutique();
        boutique.setNomBoutique(nomBoutique);
        boutique.setAdresse(adresse);
        boutique.setEntreprise(admin.getEntreprise());
        boutique.setCreatedAt(LocalDateTime.now());

        // Sauvegarder la boutique en base de données
        return boutiqueRepository.save(boutique);
    }


    // Récupérer toutes les boutiques d'une entreprise
    public List<Boutique> getBoutiquesByEntreprise(HttpServletRequest request) {
        // Vérifier la présence du token JWT dans l'entête de la requête
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        // Extraire l'ID de l'utilisateur depuis le token
        Long userId = null;
        try {
            userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'utilisateur depuis le token", e);
        }

        // Récupérer l'utilisateur par son ID
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier que l'utilisateur est bien un admin et qu'il a une entreprise associée
        if (user.getRole() == null || !user.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un admin peut récupérer les boutiques d'une entreprise !");
        }

        if (user.getEntreprise() == null) {
            throw new RuntimeException("L'Admin n'a pas d'entreprise associée.");
        }

        // Récupérer et retourner toutes les boutiques de l'entreprise
        return boutiqueRepository.findByEntrepriseId(user.getEntreprise().getId());
    }


    public Boutique updateBoutique(Long boutiqueId, String newNomBoutique, String newAdresse, HttpServletRequest request) {
        // Vérifier que l'utilisateur est un admin
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
    
        Long adminId = jwtUtil.extractUserId(token);  // Décoder l'ID de l'admin depuis le token
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));
    
        if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
            throw new RuntimeException("Seul un ADMIN peut modifier une boutique !");
        }
    
        // Vérifier si la boutique existe
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique non trouvée"));
    
        // Modifier les informations de la boutique
        if (newNomBoutique != null) boutique.setNomBoutique(newNomBoutique);
        if (newAdresse != null) boutique.setAdresse(newAdresse);
        boutique.setLastUpdated(LocalDateTime.now());
    
        // Sauvegarder les modifications
        return boutiqueRepository.save(boutique);
    }
    
}
