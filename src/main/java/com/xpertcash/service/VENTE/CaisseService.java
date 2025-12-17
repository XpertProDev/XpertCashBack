package com.xpertcash.service.VENTE;

import com.xpertcash.entity.*;
import com.xpertcash.repository.*;
import com.xpertcash.repository.VENTE.CaisseRepository;
import com.xpertcash.repository.VENTE.MouvementCaisseRepository;
import com.xpertcash.repository.VENTE.VenteRepository;
import com.xpertcash.repository.VENTE.VersementComptableRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import com.xpertcash.DTOs.VENTE.CaisseResponseDTO;
import com.xpertcash.DTOs.VENTE.DepenseRequest;
import com.xpertcash.DTOs.VENTE.DepenseResponseDTO;
import com.xpertcash.DTOs.VENTE.FermerCaisseRequest;
import com.xpertcash.DTOs.VENTE.FermerCaisseResponseDTO;
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
    @Autowired
    private VenteRepository venteRepository;

        private User getUserFromRequest(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        String userUuid = jwtUtil.extractUserUuid(token.substring(7));
        return usersRepository.findByUuid(userUuid)
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
public FermerCaisseResponseDTO fermerCaisse(FermerCaisseRequest request, HttpServletRequest httpRequest) {
    User user = getUserFromRequest(httpRequest);

    // 1️⃣ Récupérer la caisse ouverte pour cet utilisateur et cette boutique
    Caisse caisse = caisseRepository.findByVendeurIdAndStatutAndBoutiqueId(
            user.getId(), StatutCaisse.OUVERTE, request.getBoutiqueId())
            .orElseThrow(() -> new RuntimeException("Aucune caisse ouverte pour cet utilisateur dans cette boutique ou la caisse est déjà fermée."));

    // 2️⃣ Sécurité : Vérification des droits d'accès de l'utilisateur
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

    // 3️⃣ Vérifier que le montant en main est positif
    if (request.getMontantEnMain() < 0) {
        throw new RuntimeException("Le montant en main ne peut pas être négatif.");
    }

    // 4️⃣ Calculer les statistiques des dépenses
    List<MouvementCaisse> depenses = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
            caisse.getId(), TypeMouvementCaisse.DEPENSE);
    
    Double totalDepenses = depenses.stream()
            .mapToDouble(MouvementCaisse::getMontant)
            .sum();
    Integer nombreDepenses = depenses.size();

    // 5️⃣ Calculer l'écart
    Double montantTheorique = caisse.getMontantCourant();
    Double montantReel = request.getMontantEnMain();
    Double ecart = montantReel - montantTheorique;

    // 6️⃣ Mise à jour de la caisse (fermeture)
    caisse.setStatut(StatutCaisse.FERMEE);
    caisse.setDateFermeture(LocalDateTime.now());
    caisse.setMontantEnMain(montantReel);
    caisse.setEcart(ecart);
    caisseRepository.save(caisse);

    // 7️⃣ Mouvement de fermeture (enregistrement du mouvement dans la caisse)
    MouvementCaisse mouvement = new MouvementCaisse();
    mouvement.setCaisse(caisse);
    mouvement.setTypeMouvement(TypeMouvementCaisse.FERMETURE);
    mouvement.setMontant(montantReel); // Utiliser le montant réel en main
    mouvement.setDateMouvement(LocalDateTime.now());
    mouvement.setDescription("Fermeture de la caisse - Montant théorique: " + montantTheorique + 
                           ", Montant en main: " + montantReel + 
                           ", Écart: " + ecart);
    mouvementCaisseRepository.save(mouvement);

    // 8️⃣ Création du versement comptable en attente
    VersementComptable versement = new VersementComptable();
    versement.setCaisse(caisse);
    versement.setMontant(montantReel); // Utiliser le montant réel en main
    versement.setDateVersement(LocalDateTime.now());
    versement.setStatut(StatutVersement.EN_ATTENTE); // En attente
    versement.setCreePar(user);
    versementComptableRepository.save(versement);

    // 9️⃣ Créer la réponse avec toutes les informations
    FermerCaisseResponseDTO response = new FermerCaisseResponseDTO();
    response.setId(caisse.getId());
    response.setMontantInitial(caisse.getMontantInitial());
    response.setMontantCourant(montantTheorique);
    response.setMontantEnMain(montantReel);
    response.setEcart(ecart);
    response.setStatut(caisse.getStatut().name());
    response.setDateOuverture(caisse.getDateOuverture());
    response.setDateFermeture(caisse.getDateFermeture());
    response.setVendeurId(caisse.getVendeur().getId());
    response.setNomVendeur(caisse.getVendeur().getNomComplet());
    response.setBoutiqueId(caisse.getBoutique().getId());
    response.setNomBoutique(caisse.getBoutique().getNomBoutique());
    response.setTotalDepenses(totalDepenses);
    response.setNombreDepenses(nombreDepenses);

    return response;
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
                .orElseThrow(() -> new RuntimeException("Boutique inentrouvable"));

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

    //Get mes propres caisses
    public List<Caisse> getMesCaisses(Long boutiqueId, HttpServletRequest request) {
        User user = getUserFromRequest(request);
        
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        // Vérification d'appartenance à l'entreprise
        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        // Sécurité : rôle ou permission - Plus permissif que getToutesLesCaisses
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter vos caisses !");
        }

        // Récupération de toutes les caisses (ouvertes et fermées) de l'utilisateur connecté dans cette boutique
        return caisseRepository.findByVendeurIdAndBoutiqueId(user.getId(), boutiqueId);
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

        // 💰 Recalculer le montant courant à partir des mouvements de caisse
        Double montantCourant = calculerMontantCourantTheorique(caisse);
        dto.setMontantCourant(montantCourant);

        // 💳 Calculer le montant total encore en dette (ventes à crédit non totalement remboursées) pour cette caisse
        Double montantDette = calculerMontantDetteCaisse(caisse);
        dto.setMontantDette(montantDette);

        dto.setMontantEnMain(caisse.getMontantEnMain());
        dto.setEcart(caisse.getEcart());
        dto.setStatut(caisse.getStatut().name());
        dto.setDateOuverture(caisse.getDateOuverture());
        dto.setDateFermeture(caisse.getDateFermeture());
        dto.setVendeurId(caisse.getVendeur().getId());
        dto.setNomVendeur(caisse.getVendeur().getNomComplet());
        dto.setBoutiqueId(caisse.getBoutique().getId());
        dto.setNomBoutique(caisse.getBoutique().getNomBoutique());
        return dto;
    }

    /**
     * Calcule le montant théorique de la caisse à partir :
     * montantInitial
     * + ventes + ajouts
     * - retraits - remboursements
     */
    private Double calculerMontantCourantTheorique(Caisse caisse) {
        double montant = caisse.getMontantInitial() != null ? caisse.getMontantInitial() : 0.0;

        Long caisseId = caisse.getId();

        // VENTES
        List<MouvementCaisse> ventes = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                caisseId, TypeMouvementCaisse.VENTE);
        montant += ventes.stream().mapToDouble(MouvementCaisse::getMontant).sum();

        // AJOUTS
        List<MouvementCaisse> ajouts = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                caisseId, TypeMouvementCaisse.AJOUT);
        montant += ajouts.stream().mapToDouble(MouvementCaisse::getMontant).sum();

        // RETRAITS
        List<MouvementCaisse> retraits = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                caisseId, TypeMouvementCaisse.RETRAIT);
        montant -= retraits.stream().mapToDouble(MouvementCaisse::getMontant).sum();

        // REMBOURSEMENTS
        List<MouvementCaisse> remboursements = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                caisseId, TypeMouvementCaisse.REMBOURSEMENT);
        montant -= remboursements.stream().mapToDouble(MouvementCaisse::getMontant).sum();

        return montant;
    }

    /**
     * Montant de dette lié aux ventes à crédit de cette caisse:
     * somme(max(0, montantTotal - montantTotalRembourse)) pour les ventes CREDIT de cette caisse.
     */
    private Double calculerMontantDetteCaisse(Caisse caisse) {
        // Récupérer toutes les ventes de cette caisse
        List<com.xpertcash.entity.VENTE.Vente> ventes = venteRepository.findByBoutiqueId(caisse.getBoutique().getId());

        return ventes.stream()
                .filter(v -> v.getCaisse() != null && v.getCaisse().getId().equals(caisse.getId()))
                .filter(v -> v.getModePaiement() == com.xpertcash.entity.ModePaiement.CREDIT)
                .mapToDouble(v -> {
                    double total = v.getMontantTotal() != null ? v.getMontantTotal() : 0.0;
                    double rembourse = v.getMontantTotalRembourse() != null ? v.getMontantTotalRembourse() : 0.0;
                    double restant = total - rembourse;
                    return Math.max(restant, 0.0);
                })
                .sum();
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
            } else if (type == TypeMouvementCaisse.RETRAIT || type == TypeMouvementCaisse.REMBOURSEMENT) {
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

    /**
     * Enregistre une dépense depuis la caisse ouverte du vendeur
     * Permet au vendeur de faire des dépenses comme réparation de chaise, achat de matériel, etc.
     */
    @Transactional
    public CaisseResponseDTO enregistrerDepense(DepenseRequest request, HttpServletRequest httpRequest) {
        User user = getUserFromRequest(httpRequest);

        // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour effectuer une dépense !");
        }

        // Vérification de la boutique
        Boutique boutique = boutiqueRepository.findById(request.getBoutiqueId())
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));
        
        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        // Vérifier qu'une caisse OUVERTE existe pour ce vendeur/boutique
        Caisse caisse = caisseRepository.findByVendeurIdAndStatutAndBoutiqueId(
                user.getId(), StatutCaisse.OUVERTE, request.getBoutiqueId())
                .orElseThrow(() -> new RuntimeException("Aucune caisse ouverte pour ce vendeur dans cette boutique. Veuillez ouvrir une caisse avant de faire une dépense."));

        // Vérifier que le montant de la dépense ne dépasse pas le montant disponible
        if (request.getMontant() > caisse.getMontantCourant()) {
            throw new RuntimeException("Montant insuffisant dans la caisse. Montant disponible: " + caisse.getMontantCourant() + ", Montant demandé: " + request.getMontant());
        }

        // Vérifier que le montant est positif
        if (request.getMontant() <= 0) {
            throw new RuntimeException("Le montant de la dépense doit être positif.");
        }

        // Créer le mouvement de dépense
        MouvementCaisse mouvement = new MouvementCaisse();
        mouvement.setCaisse(caisse);
        mouvement.setTypeMouvement(TypeMouvementCaisse.DEPENSE);
        mouvement.setMontant(request.getMontant());
        mouvement.setDateMouvement(LocalDateTime.now());
        mouvement.setDescription("Dépense: " + request.getMotif());
        mouvementCaisseRepository.save(mouvement);

        // Mettre à jour le montant courant de la caisse (diminuer)
        caisse.setMontantCourant(caisse.getMontantCourant() - request.getMontant());
        caisseRepository.save(caisse);

        // Retourner l'état actuel de la caisse
        return mapToCaisseResponseDTO(caisse);
    }

    /**
     * Liste toutes les dépenses d'une caisse spécifique
     */
    public List<DepenseResponseDTO> listerDepensesCaisse(Long caisseId, HttpServletRequest request) {
        User user = getUserFromRequest(request);

        // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les dépenses !");
        }

        // Récupérer la caisse
        Caisse caisse = caisseRepository.findById(caisseId)
                .orElseThrow(() -> new RuntimeException("Caisse introuvable"));

        // Vérification d'appartenance à l'entreprise
        if (!caisse.getBoutique().getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette caisse n'appartient pas à votre entreprise.");
        }

        // Si c'est un vendeur, il ne peut voir que ses propres dépenses
        if (!isAdminOrManager && !caisse.getVendeur().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à consulter les dépenses de cette caisse.");
        }

        // Récupérer tous les mouvements de type DEPENSE pour cette caisse
        List<MouvementCaisse> mouvements = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                caisseId, TypeMouvementCaisse.DEPENSE);

        // Transformer en DTO
        List<DepenseResponseDTO> depenses = new ArrayList<>();
        for (MouvementCaisse mouvement : mouvements) {
            DepenseResponseDTO dto = new DepenseResponseDTO();
            dto.setId(mouvement.getId());
            dto.setCaisseId(mouvement.getCaisse().getId());
            dto.setMontant(mouvement.getMontant());
            dto.setDescription(mouvement.getDescription());
            dto.setDateMouvement(mouvement.getDateMouvement());
            dto.setNomVendeur(mouvement.getCaisse().getVendeur().getNomComplet());
            dto.setNomBoutique(mouvement.getCaisse().getBoutique().getNomBoutique());
            depenses.add(dto);
        }

        return depenses;
    }

    /**
     * Liste toutes les dépenses d'un vendeur dans une boutique
     */
    public List<DepenseResponseDTO> listerDepensesVendeur(Long boutiqueId, HttpServletRequest request) {
        User user = getUserFromRequest(request);

        // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les dépenses !");
        }

        // Vérification de la boutique
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        // Récupérer toutes les caisses du vendeur dans cette boutique
        List<Caisse> caisses = caisseRepository.findByVendeurIdAndBoutiqueId(user.getId(), boutiqueId);

        List<DepenseResponseDTO> toutesDepenses = new ArrayList<>();
        for (Caisse caisse : caisses) {
            List<MouvementCaisse> mouvements = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                    caisse.getId(), TypeMouvementCaisse.DEPENSE);
            
            for (MouvementCaisse mouvement : mouvements) {
                DepenseResponseDTO dto = new DepenseResponseDTO();
                dto.setId(mouvement.getId());
                dto.setCaisseId(mouvement.getCaisse().getId());
                dto.setMontant(mouvement.getMontant());
                dto.setDescription(mouvement.getDescription());
                dto.setDateMouvement(mouvement.getDateMouvement());
                dto.setNomVendeur(mouvement.getCaisse().getVendeur().getNomComplet());
                dto.setNomBoutique(mouvement.getCaisse().getBoutique().getNomBoutique());
                toutesDepenses.add(dto);
            }
        }

        return toutesDepenses;
    }

}