package com.xpertcash.composant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class Utilitaire {

     @Autowired
    private JwtUtil jwtUtil; 

      @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    public User getAuthenticatedUser(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        String userUuid;
        try {
            userUuid = jwtUtil.extractUserUuid(token.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'UUID utilisateur", e);
        }

        if (userUuid == null) {
            throw new RuntimeException("UUID utilisateur non trouvé dans le token");
        }

        return usersRepository.findByUuid(userUuid)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec UUID: " + userUuid));
    }


        public Boutique validateAdminOrManagerAccess(Long boutiqueId, User user) {
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les ventes !");
        }

        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        return boutique;
    }

    //verifier si son Entreprise est active
    public boolean isEntrepriseActive(Long entrepriseId) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));
        return Boolean.TRUE.equals(entreprise.getActive());

    }

}
