package com.xpertcash.service.VENTE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xpertcash.DTOs.VENTE.VersementComptableDTO;
import com.xpertcash.composant.Utilitaire;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.VENTE.StatutVersement;
import com.xpertcash.entity.VENTE.VersementComptable;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.FactureVenteRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.repository.VENTE.VenteHistoriqueRepository;
import com.xpertcash.repository.VENTE.VenteProduitRepository;
import com.xpertcash.repository.VENTE.VenteRepository;
import com.xpertcash.repository.VENTE.VersementComptableRepository;

import jakarta.servlet.http.HttpServletRequest;
import com.xpertcash.service.AuthenticationHelper;

@Service
public class VersementComptableService {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private VersementComptableRepository versementComptableRepository;
    @Autowired
    private BoutiqueRepository boutiqueRepository;
    @Autowired
    private UsersRepository usersRepository;
    
  
    @Autowired
    private JwtUtil jwtUtil;
 

    @Transactional
    public List<VersementComptableDTO> getVersementsDeBoutique(Long boutiqueId, HttpServletRequest request) {
        // 🔐 Vérification du token JWT
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long userId;
        try {
            userId = jwtUtil.extractUserId(token.substring(7));
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token", e);
        }

        // Récupération de l'utilisateur connecté
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (user.getEntreprise() == null) {
            throw new RuntimeException("L'utilisateur connecté n'appartient à aucune entreprise.");
        }

        // Vérification des rôles
        RoleType role = user.getRole().getName();
        boolean isAdminManagerOrComptable = 
            role == RoleType.ADMIN || role == RoleType.MANAGER || role == RoleType.COMPTABLE;

        if (!isAdminManagerOrComptable) {
            throw new RuntimeException("Vous n'avez pas les droits pour voir les versements.");
        }

        // Vérification que la boutique appartient à l'entreprise de l'utilisateur
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));
        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Cette boutique n'appartient pas à votre entreprise.");
        }

        // 📥 Récupérer tous les versements de cette boutique
       return versementComptableRepository.findByCaisse_BoutiqueId(boutiqueId).stream()
        .map(v -> {
            VersementComptableDTO dto = new VersementComptableDTO();
            dto.setId(v.getId());
            dto.setMontantVerse(v.getMontant());
            dto.setMontantInitialCaisse(v.getCaisse().getMontantInitial());
            dto.setMontantCourantCaisse(v.getCaisse().getMontantCourant());
            dto.setDateVersement(v.getDateVersement());
            dto.setStatut(v.getStatut().name());
            dto.setCaisseId(v.getCaisse().getId());
            dto.setBoutiqueId(v.getCaisse().getBoutique().getId());
            dto.setNomBoutique(v.getCaisse().getBoutique().getNomBoutique());
            dto.setNomVendeur(v.getCreePar().getNomComplet());

            // Gérer le cas où ValidePar est null
            if (v.getValidePar() != null) {
                dto.setNomComptable(v.getValidePar().getNomComplet());
                dto.setDateValidation(v.getDateValidation());
            } else {
                // Valeur par défaut pour "NomComptable" si ValidePar est null
                dto.setNomComptable("Non validé");
                dto.setDateValidation(null);
            }

            return dto;
        })

    .collect(Collectors.toList());

    }


    @Transactional
    public VersementComptableDTO validerVersement(Long versementId, boolean valide, HttpServletRequest request) {
        // 🔐 Vérification du token JWT
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        if (user.getEntreprise() == null) {
            throw new RuntimeException("L'utilisateur connecté n'appartient à aucune entreprise.");
        }

        // 🔐 Vérification des rôles
        RoleType role = user.getRole().getName();
        boolean isAdminOrManagerOrComptable =
                role == RoleType.ADMIN || role == RoleType.MANAGER || role == RoleType.COMPTABLE;
        if (!isAdminOrManagerOrComptable) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour valider ou refuser un versement.");
        }

        // 🔍 Récupération du versement
        VersementComptable versement = versementComptableRepository.findById(versementId)
                .orElseThrow(() -> new RuntimeException("Versement introuvable"));

        // 🔍 Vérification que la boutique appartient bien à l'entreprise de l'utilisateur
        if (!versement.getCaisse().getBoutique().getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Ce versement n'appartient pas à votre entreprise.");
        }

        // ✅ Mise à jour du statut et des infos de validation
        versement.setStatut(valide ? StatutVersement.VALIDE : StatutVersement.REFUSE);
        versement.setDateValidation(LocalDateTime.now());
        versement.setValidePar(user);

        VersementComptable saved = versementComptableRepository.save(versement);

        // 📝 Construction du DTO
        VersementComptableDTO dto = new VersementComptableDTO();
        dto.setId(saved.getId());
        dto.setMontantInitialCaisse(saved.getCaisse().getMontantInitial());
        dto.setMontantCourantCaisse(saved.getCaisse().getMontantCourant());
        dto.setMontantVerse(saved.getMontant());
        dto.setDateVersement(saved.getDateVersement());
        dto.setStatut(saved.getStatut().name());
        dto.setCaisseId(saved.getCaisse().getId());
        dto.setBoutiqueId(saved.getCaisse().getBoutique().getId());
        dto.setNomBoutique(saved.getCaisse().getBoutique().getNomBoutique());
        dto.setNomVendeur(saved.getCaisse().getVendeur().getNomComplet());
        dto.setNomComptable(saved.getValidePar() != null ? saved.getValidePar().getNomComplet() : null);
        dto.setDateValidation(saved.getDateValidation());

        return dto;
    }



    @Transactional
public List<VersementComptableDTO> getVersementsParStatut(Long boutiqueId, StatutVersement statut, HttpServletRequest request) {
    // 🔐 Vérification JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    if (user.getEntreprise() == null) {
        throw new RuntimeException("L'utilisateur connecté n'appartient à aucune entreprise.");
    }

    // Vérification des droits
    RoleType role = user.getRole().getName();
    boolean isAdminManagerOrComptable = 
        role == RoleType.ADMIN || role == RoleType.MANAGER || role == RoleType.COMPTABLE;
    if (!isAdminManagerOrComptable) {
        throw new RuntimeException("Vous n'avez pas les droits pour voir ces versements.");
    }

    // 🔍 Vérification que la boutique appartient à l’entreprise
    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique introuvable"));
    if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Cette boutique n'appartient pas à votre entreprise.");
    }

    // 📥 Récupération filtrée
    List<VersementComptable> versements = versementComptableRepository
            .findByCaisse_BoutiqueIdAndStatut(boutiqueId, statut);

    // 📝 Conversion en DTO
    return versements.stream().map(v -> {
        VersementComptableDTO dto = new VersementComptableDTO();
        dto.setId(v.getId());
        dto.setMontantInitialCaisse(v.getCaisse().getMontantInitial());
        dto.setMontantCourantCaisse(v.getCaisse().getMontantCourant());
        dto.setMontantVerse(v.getMontant());
        dto.setDateVersement(v.getDateVersement());
        dto.setStatut(v.getStatut().name());
        dto.setCaisseId(v.getCaisse().getId());
        dto.setBoutiqueId(v.getCaisse().getBoutique().getId());
        dto.setNomBoutique(v.getCaisse().getBoutique().getNomBoutique());
        dto.setNomVendeur(v.getCaisse().getVendeur().getNomComplet());
        dto.setNomComptable(v.getValidePar() != null ? v.getValidePar().getNomComplet() : null);
        dto.setDateValidation(v.getDateValidation());
        return dto;
    }).toList();
}


}
