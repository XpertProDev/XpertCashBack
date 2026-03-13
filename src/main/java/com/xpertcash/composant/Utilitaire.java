package com.xpertcash.composant;

import java.text.Normalizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.repository.BoutiqueRepository;
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
        String n = normalizeCountry(p);
        switch (n) {
            case "mali": case "ml": return "+223";
            case "senegal": case "sn": return "+221";
            case "cote d ivoire": case "cote d'ivoire": case "ci": return "+225";
            case "burkina faso": case "bf": return "+226";
            case "niger": case "ne": return "+227";
            case "france": case "fr": return "+33";
            case "belgique": case "be": return "+32";
            case "suisse": case "ch": return "+41";
            case "canada": case "ca": return "+1";
            case "etats unis": case "etatsunis": case "usa": case "us": return "+1";
            case "maroc": case "ma": return "+212";
            case "algerie": case "dz": return "+213";
            case "tunisie": case "tn": return "+216";
            case "mauritanie": case "mr": return "+222";
            case "sierra leone": case "sl": return "+232";
            case "liberia": case "lr": return "+231";
            case "guinee bissau": case "guinee-bissau": case "gw": return "+245";
            case "togo": case "tg": return "+228";
            case "benin": case "bj": return "+229";
            case "guinee": case "gn": return "+224";
            case "tchad": case "td": return "+235";
            case "cameroun": case "cm": return "+237";
            case "rdc": case "republique democratique du congo": case "cd": return "+243";
            case "congo": case "cg": return "+242";
            case "gabon": case "ga": return "+241";
            case "afrique du sud": case "za": return "+27";
            case "rwanda": case "rw": return "+250";
            case "kenya": case "ke": return "+254";
            case "nigeria": case "ng": return "+234";
            case "ghana": case "gh": return "+233";
            case "ethiopie": case "et": return "+251";
            case "egypte": case "eg": return "+20";
            case "inde": case "in": return "+91";
            case "chine": case "cn": return "+86";
            case "mexique": case "mx": return "+52";
            case "allemagne": case "de": return "+49";
            case "espagne": case "es": return "+34";
            case "italie": case "it": return "+39";
            case "royaume uni": case "uk": case "gb": return "+44";
            case "pays bas": case "nl": return "+31";
            case "portugal": case "pt": return "+351";
            case "turquie": case "tr": return "+90";
            default: break;
        }

        // Fallback par détection partielle
        if (n.contains("mali")) return "+223";
        if (n.contains("senegal")) return "+221";
        if (n.contains("ivoire")) return "+225";
        if (n.contains("burkina")) return "+226";
        if (n.equals("niger")) return "+227";
        if (n.contains("france")) return "+33";
        if (n.contains("belgique")) return "+32";
        if (n.contains("suisse")) return "+41";
        if (n.contains("canada")) return "+1";
        if (n.contains("etats unis") || n.contains("usa")) return "+1";
        if (n.contains("maroc")) return "+212";
        if (n.contains("algerie")) return "+213";
        if (n.contains("tunisie")) return "+216";
        if (n.contains("mauritanie")) return "+222";
        if (n.contains("sierra leone")) return "+232";
        if (n.contains("liberia")) return "+231";
        if (n.contains("guinee bissau")) return "+245";
        if (n.contains("togo")) return "+228";
        if (n.contains("benin")) return "+229";
        if (n.contains("guinee")) return "+224";
        if (n.contains("tchad")) return "+235";
        if (n.contains("cameroun")) return "+237";
        if (n.contains("rdc") || n.contains("republique democratique du congo")) return "+243";
        if (n.equals("congo")) return "+242";
        if (n.contains("gabon")) return "+241";
        if (n.contains("afrique du sud")) return "+27";
        if (n.contains("rwanda")) return "+250";
        if (n.contains("kenya")) return "+254";
        if (n.contains("nigeria")) return "+234";
        if (n.contains("ghana")) return "+233";
        if (n.contains("ethiopie")) return "+251";
        if (n.contains("egypte")) return "+20";
        if (n.contains("inde")) return "+91";
        if (n.contains("chine")) return "+86";
        if (n.contains("mexique")) return "+52";
        if (n.contains("allemagne")) return "+49";
        if (n.contains("espagne")) return "+34";
        if (n.contains("italie")) return "+39";
        if (n.contains("royaume uni")) return "+44";
        if (n.contains("pays bas")) return "+31";
        if (n.contains("portugal")) return "+351";
        if (n.contains("turquie")) return "+90";
        return null;
    }

    private static String normalizeCountry(String value) {
        if (value == null) return "";
        String s = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .replace("’", "'")
                .replaceAll("[^a-z0-9']", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return s;
    }

}
