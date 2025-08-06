package com.xpertcash.service.VENTE;

import com.xpertcash.entity.*;
import com.xpertcash.repository.*;
import com.xpertcash.repository.VENTE.CaisseRepository;
import com.xpertcash.repository.VENTE.MouvementCaisseRepository;
import com.xpertcash.repository.VENTE.VersementComptableRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import com.xpertcash.DTOs.VENTE.CaisseResponseDTO;
import com.xpertcash.composant.Utilitaire;
import com.xpertcash.configuration.JwtUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.VENTE.MouvementCaisse;
import com.xpertcash.entity.VENTE.StatutCaisse;
import com.xpertcash.entity.VENTE.StatutVersement;
import com.xpertcash.entity.VENTE.TypeMouvementCaisse;
import com.xpertcash.entity.VENTE.Vente;
import com.xpertcash.entity.VENTE.VersementComptable;

@Service
public class CaisseService {
    @Autowired
    private CaisseRepository caisseRepository;
    @Autowired
    private MouvementCaisseRepository mouvementCaisseRepository;
    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private BoutiqueRepository boutiqueRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private Utilitaire utilitaire;
    @Autowired
    private VersementComptableRepository versementComptableRepository;

        private User getUserFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        String jwtToken = token.substring(7);
        Long userId = jwtUtil.extractUserId(jwtToken);
        return usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }


   @Transactional
    public Caisse ouvrirCaisse(Long boutiqueId, Double montantInitial, HttpServletRequest request) {
        User user = getUserFromRequest(request);

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        // Sécurité : rôle ou permission
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour ouvrir une caisse !");
        }

        // Vérification d'appartenance à l'entreprise
        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        // Vérifier qu'il n'y a pas déjà une caisse ouverte pour ce vendeur/boutique
        if (caisseRepository.existsByBoutiqueIdAndVendeurIdAndStatut(boutiqueId, user.getId(), StatutCaisse.OUVERTE)) {
            throw new RuntimeException("Une caisse est déjà ouverte pour ce vendeur dans cette boutique.");
        }

        // Création d'une nouvelle caisse (historique)
        Caisse caisse = new Caisse();
        caisse.setBoutique(boutique);
        caisse.setVendeur(user);
        caisse.setMontantInitial(montantInitial != null ? montantInitial : 0.0);
        caisse.setMontantCourant(montantInitial != null ? montantInitial : 0.0);
        caisse.setStatut(StatutCaisse.OUVERTE);
        caisse.setDateOuverture(LocalDateTime.now());

        return caisseRepository.save(caisse);
    }

   @Transactional
public Caisse fermerCaisse(Long caisseId, HttpServletRequest request) {
    User user = getUserFromRequest(request);

    // 1️⃣ Charger la caisse ouverte
    Caisse caisse = caisseRepository.findByIdAndStatut(caisseId, StatutCaisse.OUVERTE)
            .orElseThrow(() -> new RuntimeException("Caisse introuvable ou déjà fermée."));

    // 2️⃣ Sécurité
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits nécessaires pour fermer une caisse !");
    }

    if (!caisse.getBoutique().getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
    }

    if (!isAdminOrManager && !caisse.getVendeur().getId().equals(user.getId())) {
        throw new RuntimeException("Vous n'êtes pas autorisé à fermer cette caisse.");
    }

    // 3️⃣ Mise à jour de la caisse
    caisse.setStatut(StatutCaisse.FERMEE);
    caisse.setDateFermeture(LocalDateTime.now());
    caisseRepository.save(caisse);

    // 4️⃣ Mouvement de fermeture
    MouvementCaisse mouvement = new MouvementCaisse();
    mouvement.setCaisse(caisse);
    mouvement.setTypeMouvement(TypeMouvementCaisse.FERMETURE);
    mouvement.setMontant(caisse.getMontantCourant());
    mouvement.setDateMouvement(LocalDateTime.now());
    mouvement.setDescription("Fermeture de la caisse");
    mouvementCaisseRepository.save(mouvement);

    // 5️⃣ Création du versement comptable en attente
    VersementComptable versement = new VersementComptable();
    versement.setCaisse(caisse);
    versement.setMontant(caisse.getMontantCourant());
    versement.setDateVersement(LocalDateTime.now());
    versement.setStatut(StatutVersement.EN_ATTENTE); // Enum à créer
    versement.setCreePar(user);
    versementComptableRepository.save(versement);

    return caisse;
}

  
    //Get caisse ouvert
    public Optional<Caisse> getCaisseActive(Long boutiqueId, HttpServletRequest request) {
    User user = getUserFromRequest(request);

    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

    // Sécurité : rôle ou permission
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter la caisse !");
    }

    // Vérification d'appartenance à l'entreprise
    if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
    }

    // Requête optimisée
    return caisseRepository.findFirstByBoutiqueIdAndVendeurIdAndStatut(
            boutiqueId,
            user.getId(),
            StatutCaisse.OUVERTE
    );
}


    //Get tout les caisse ouvert
    public List<Caisse> getCaissesActivesBoutique(Long boutiqueId, HttpServletRequest request) {
    User user = getUserFromRequest(request);
    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

    // Sécurité : rôle ou permission
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    if (!isAdminOrManager) {
        throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les caisses !");
    }

    // Vérification d'appartenance à l'entreprise
    if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
    }

    // Récupération optimisée depuis la base
    return caisseRepository.findByBoutiqueIdAndStatut(boutiqueId, StatutCaisse.OUVERTE);
}


    //Get tout les caisse
    public List<Caisse> getToutesLesCaisses(Long boutiqueId, HttpServletRequest request) {
        User user = getUserFromRequest(request);
        
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        // Vérification d'appartenance à l'entreprise
        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        // Sécurité : rôle ou permission
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les caisses !");
        }

        return caisseRepository.findByBoutiqueId(boutiqueId);
    }

    // Le vendeur get sa derniere caisse a lui
    public Optional<CaisseResponseDTO> getDerniereCaisseVendeur(Long boutiqueId, HttpServletRequest request) {
    User user = getUserFromRequest(request);

    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

    // Sécurité : rôle ou permission
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter la caisse !");
    }

    // Vérification d'appartenance à l'entreprise
    if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
    }

    // Requête pour la dernière caisse (ouverte ou fermée)
    Optional<Caisse> caisseOpt = caisseRepository.findTopByBoutiqueIdAndVendeurIdOrderByDateOuvertureDesc(
            boutiqueId,
            user.getId()
    );

    return caisseOpt.map(caisse -> {
        CaisseResponseDTO dto = new CaisseResponseDTO();
        dto.setId(caisse.getId());
        dto.setMontantInitial(caisse.getMontantInitial());
        dto.setMontantCourant(caisse.getMontantCourant());
        dto.setStatut(caisse.getStatut().name());
        dto.setDateOuverture(caisse.getDateOuverture());
        dto.setDateFermeture(caisse.getDateFermeture());
        dto.setVendeurId(caisse.getVendeur().getId());
        dto.setNomVendeur(caisse.getVendeur().getNomComplet());
        dto.setBoutiqueId(caisse.getBoutique().getId());
        dto.setNomBoutique(caisse.getBoutique().getNomBoutique());
        return dto;
    });
}

    // Suivre la "fluidité d’argent" en cours d’activité.
    public CaisseResponseDTO getEtatActuelCaisse(Long boutiqueId, HttpServletRequest request) {
    User user = getUserFromRequest(request);

    Boutique boutique = boutiqueRepository.findById(boutiqueId)
        .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

    // Vérif rôle/permission
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter la caisse !");
    }

    if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
    }

    Caisse caisse = caisseRepository.findFirstByBoutiqueIdAndVendeurIdOrderByDateOuvertureDesc(
        boutiqueId, user.getId())
        .orElseThrow(() -> new RuntimeException("Aucune caisse trouvée pour ce vendeur"));

    return mapToCaisseResponseDTO(caisse);
}
    public CaisseResponseDTO mapToCaisseResponseDTO(Caisse caisse) {
        CaisseResponseDTO dto = new CaisseResponseDTO();
        dto.setId(caisse.getId());
        dto.setMontantInitial(caisse.getMontantInitial());
        dto.setMontantCourant(caisse.getMontantCourant());
        dto.setStatut(caisse.getStatut().name());
        dto.setDateOuverture(caisse.getDateOuverture());
        dto.setDateFermeture(caisse.getDateFermeture());
        dto.setVendeurId(caisse.getVendeur().getId());
        dto.setNomVendeur(caisse.getVendeur().getNomComplet());
        dto.setBoutiqueId(caisse.getBoutique().getId());
        dto.setNomBoutique(caisse.getBoutique().getNomBoutique());
        return dto;
    }





    @Transactional
    public void ajouterMouvement(Caisse caisse, TypeMouvementCaisse type, Double montant, String description, Vente vente, ModePaiement modePaiement, Double montantPaye) {
        MouvementCaisse mouvement = new MouvementCaisse();
        mouvement.setCaisse(caisse);
        mouvement.setTypeMouvement(type);
        mouvement.setMontant(montant);
        mouvement.setDateMouvement(LocalDateTime.now());
        mouvement.setDescription(description);
        mouvement.setVente(vente);
        mouvement.setModePaiement(modePaiement);
        mouvement.setMontantPaye(montantPaye);
        mouvementCaisseRepository.save(mouvement);
        // Mettre à jour le montant courant de la caisse
        if (type == TypeMouvementCaisse.VENTE || type == TypeMouvementCaisse.AJOUT) {
            caisse.setMontantCourant(caisse.getMontantCourant() + montant);
        } else if (type == TypeMouvementCaisse.RETRAIT) {
            caisse.setMontantCourant(caisse.getMontantCourant() - montant);
        }
        caisseRepository.save(caisse);
    }


    // Get caisses d'un vendeur
    public List<CaisseResponseDTO> getCaissesByVendeur(Long vendeurId, HttpServletRequest request) {
    // 1️⃣ Récupération de l'utilisateur connecté
    User user = utilitaire.getAuthenticatedUser(request);

    // 2️⃣ Récupération du vendeur ciblé
    User vendeur = usersRepository.findById(vendeurId)
            .orElseThrow(() -> new RuntimeException("Vendeur introuvable"));

    // 3️⃣ Sécurité : Admin et Manager peuvent voir toutes les caisses du vendeur
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;

    if (!isAdminOrManager) {
        // Si c'est un vendeur, il ne peut voir que ses propres caisses
        if (!user.getId().equals(vendeurId)) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les caisses de ce vendeur !");
        }
    }

    // 4️⃣ Vérification que le vendeur appartient à la même entreprise
    if (!vendeur.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : ce vendeur n'appartient pas à votre entreprise.");
    }

    // 5️⃣ Récupération de toutes les caisses (ouvertes et fermées) du vendeur
    List<Caisse> caisses = caisseRepository.findByVendeurId(vendeurId);

    // 6️⃣ Transformation en DTO
    List<CaisseResponseDTO> responses = new ArrayList<>();
    for (Caisse caisse : caisses) {
        CaisseResponseDTO dto = new CaisseResponseDTO();
        dto.setId(caisse.getId());
        dto.setMontantInitial(caisse.getMontantInitial());
        dto.setMontantCourant(caisse.getMontantCourant());
        dto.setStatut(caisse.getStatut().name());
        dto.setDateOuverture(caisse.getDateOuverture());
        dto.setDateFermeture(caisse.getDateFermeture());
        dto.setVendeurId(caisse.getVendeur() != null ? caisse.getVendeur().getId() : null);
        dto.setNomVendeur(caisse.getVendeur() != null ? caisse.getVendeur().getNomComplet() : null);
        dto.setBoutiqueId(caisse.getBoutique() != null ? caisse.getBoutique().getId() : null);
        dto.setNomBoutique(caisse.getBoutique() != null ? caisse.getBoutique().getNomBoutique() : null);
        responses.add(dto);
    }

    return responses;
}




}