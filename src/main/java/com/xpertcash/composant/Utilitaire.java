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
    // public boolean isEntrepriseActive(Long entrepriseId) {
    //     Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
    //             .orElseThrow(() -> new RuntimeException("Entreprise introuvable"));
    //     return Boolean.TRUE.equals(entreprise.getActive());

    // }

    /**
     * Retourne l'indicatif téléphonique international pour un pays (ex: Mali -> "+223").
     * Accepte le nom du pays, le code ISO (2 lettres) ou un indicatif déjà formaté (+223, 223).
     */
    public static String getIndicatifPays(String pays) {
        if (pays == null || pays.isBlank()) return null;
        String p = pays.trim();
        if (p.isEmpty()) return null;
        // Déjà au format +XXX
        if (p.startsWith("+") && p.length() > 1 && p.substring(1).matches("[0-9]+")) return p;
        // Uniquement des chiffres (ex: 223)
        if (p.matches("[0-9]+")) return "+" + p;
        String lower = p.toLowerCase();
        switch (lower) {
            case "mali": case "ml": return "+223";
            case "sénégal": case "senegal": case "sn": return "+221";
            case "guinée": case "guinee": case "gn": return "+224";
            case "burkina faso": case "bf": return "+226";
            case "côte d'ivoire": case "cote d'ivoire": case "ci": case "ivoire": return "+225";
            case "bénin": case "benin": case "bj": return "+229";
            case "togo": case "tg": return "+228";
            case "niger": case "ne": return "+227";
            case "ghana": case "gh": return "+233";
            case "nigeria": case "ng": return "+234";
            case "cameroun": case "cm": return "+237";
            case "tchad": case "td": return "+235";
            case "gabon": case "ga": return "+241";
            case "congo": case "cg": return "+242";
            case "rdc": case "république démocratique du congo": case "cd": return "+243";
            case "france": case "fr": return "+33";
            default: break;
        }
        // Fallback: si le libellé contient le nom du pays (ex: "République du Mali", "Mali ")
        if (lower.contains("mali")) return "+223";
        if (lower.contains("senegal") || lower.contains("sénégal")) return "+221";
        if (lower.contains("guinee") || lower.contains("guinée")) return "+224";
        if (lower.contains("burkina")) return "+226";
        if (lower.contains("ivoire") || lower.contains("côte d") || lower.contains("cote d")) return "+225";
        if (lower.contains("benin") || lower.contains("bénin")) return "+229";
        if (lower.contains("togo")) return "+228";
        if (lower.contains("niger") && !lower.contains("nigeria")) return "+227";
        if (lower.contains("ghana")) return "+233";
        if (lower.contains("nigeria")) return "+234";
        if (lower.contains("cameroun")) return "+237";
        if (lower.contains("tchad")) return "+235";
        if (lower.contains("gabon")) return "+241";
        if (lower.contains("congo") || lower.contains("rdc")) return "+243";
        if (lower.contains("france")) return "+33";
        return null;
    }

}
