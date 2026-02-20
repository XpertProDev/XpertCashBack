package com.xpertcash.service.VENTE;

import com.xpertcash.entity.*;
import com.xpertcash.repository.*;
import com.xpertcash.repository.VENTE.CaisseRepository;
import com.xpertcash.repository.VENTE.MouvementCaisseRepository;
import com.xpertcash.repository.VENTE.VenteRepository;
import com.xpertcash.repository.VENTE.VersementComptableRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;

import com.xpertcash.DTOs.PaginatedResponseDTO;
import com.xpertcash.DTOs.VENTE.CaisseResponseDTO;
import com.xpertcash.DTOs.VENTE.DepenseRequest;
import com.xpertcash.DTOs.VENTE.DepenseResponseDTO;
import com.xpertcash.DTOs.VENTE.FermerCaisseRequest;
import com.xpertcash.DTOs.VENTE.FermerCaisseResponseDTO;
import com.xpertcash.composant.Utilitaire;
import com.xpertcash.configuration.JwtUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour ouvrir une caisse !");
        }

        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        // Vérifier qu'il n'y a pas déjà une caisse ouverte pour ce vendeur/boutique
        if (caisseRepository.existsByBoutiqueIdAndVendeurIdAndStatut(boutiqueId, user.getId(), StatutCaisse.OUVERTE)) {
            throw new RuntimeException("Une caisse est déjà ouverte pour ce vendeur dans cette boutique.");
        }

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

    //  Récupérer la caisse ouverte pour cet utilisateur et cette boutique
    Caisse caisse = caisseRepository.findByVendeurIdAndStatutAndBoutiqueId(
            user.getId(), StatutCaisse.OUVERTE, request.getBoutiqueId())
            .orElseThrow(() -> new RuntimeException("Aucune caisse ouverte pour cet utilisateur dans cette boutique ou la caisse est déjà fermée."));

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

    if (request.getMontantEnMain() < 0) {
        throw new RuntimeException("Le montant en main ne peut pas être négatif.");
    }

    List<MouvementCaisse> depenses = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
            caisse.getId(), TypeMouvementCaisse.DEPENSE);
    
    Double totalDepenses = depenses.stream()
            .mapToDouble(MouvementCaisse::getMontant)
            .sum();
    Integer nombreDepenses = depenses.size();

    //  Calculer l'écart
    Double montantTheorique = caisse.getMontantCourant();
    Double montantReel = request.getMontantEnMain();
    Double ecart = montantReel - montantTheorique;

    caisse.setStatut(StatutCaisse.FERMEE);
    caisse.setDateFermeture(LocalDateTime.now());
    caisse.setMontantEnMain(montantReel);
    caisse.setEcart(ecart);
    caisseRepository.save(caisse);

    MouvementCaisse mouvement = new MouvementCaisse();
    mouvement.setCaisse(caisse);
    mouvement.setTypeMouvement(TypeMouvementCaisse.FERMETURE);
    mouvement.setMontant(montantReel);
    mouvement.setDateMouvement(LocalDateTime.now());
    mouvement.setDescription("Fermeture de la caisse - Montant théorique: " + montantTheorique + 
                           ", Montant en main: " + montantReel + 
                           ", Écart: " + ecart);
    mouvementCaisseRepository.save(mouvement);

    VersementComptable versement = new VersementComptable();
    versement.setCaisse(caisse);
    versement.setMontant(montantReel);
    versement.setDateVersement(LocalDateTime.now());
    versement.setStatut(StatutVersement.EN_ATTENTE);
    versement.setCreePar(user);
    versementComptableRepository.save(versement);

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

  
    public Optional<Caisse> getCaisseActive(Long boutiqueId, HttpServletRequest request) {
    User user = getUserFromRequest(request);

    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter la caisse !");
    }

    if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
    }

    return caisseRepository.findFirstByBoutiqueIdAndVendeurIdAndStatut(
            boutiqueId,
            user.getId(),
            StatutCaisse.OUVERTE
    );
}


    public List<Caisse> getCaissesActivesBoutique(Long boutiqueId, HttpServletRequest request) {
    User user = getUserFromRequest(request);
    Boutique boutique = boutiqueRepository.findById(boutiqueId)
            .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    if (!isAdminOrManager) {
        throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les caisses !");
    }

    if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
    }

    return caisseRepository.findByBoutiqueIdAndStatut(boutiqueId, StatutCaisse.OUVERTE);
}


    /**
     * Liste paginée de toutes les caisses d'une boutique (admin/manager, côté base).
     */
    public PaginatedResponseDTO<Caisse> getToutesLesCaissesPaginated(Long boutiqueId, HttpServletRequest request,
            int page, int size, String sortBy, String sortDir) {
        User user = getUserFromRequest(request);
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));
        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les caisses !");
        }
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir != null ? sortDir : "asc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String property = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim() : "dateOuverture";
        Sort sort = Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Caisse> caissePage = caisseRepository.findByBoutiqueIdWithVendeurAndBoutique(boutiqueId, pageable);
        return PaginatedResponseDTO.fromPage(caissePage);
    }

    /** Retourne la première page (20 éléments par défaut) pour compatibilité. */
    public List<Caisse> getToutesLesCaisses(Long boutiqueId, HttpServletRequest request) {
        return getToutesLesCaissesPaginated(boutiqueId, request, 0, 20, "dateOuverture", "desc").getContent();
    }

    /**
     * Liste paginée des caisses du vendeur pour une boutique (côté base).
     */
    public PaginatedResponseDTO<Caisse> getMesCaissesPaginated(Long boutiqueId, HttpServletRequest request,
            int page, int size, String sortBy, String sortDir) {
        User user = getUserFromRequest(request);
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));
        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter vos caisses !");
        }
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;

        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir != null ? sortDir : "asc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        String property = (sortBy != null && !sortBy.isBlank()) ? sortBy.trim() : "dateOuverture";
        Sort sort = Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Caisse> caissePage = caisseRepository.findByVendeurIdAndBoutiqueIdWithVendeurAndBoutique(user.getId(), boutiqueId, pageable);
        return PaginatedResponseDTO.fromPage(caissePage);
    }

    /** Retourne la première page (20 éléments par défaut) pour compatibilité. */
    public List<Caisse> getMesCaisses(Long boutiqueId, HttpServletRequest request) {
        return getMesCaissesPaginated(boutiqueId, request, 0, 20, "dateOuverture", "desc").getContent();
    }

    public Optional<CaisseResponseDTO> getDerniereCaisseVendeur(Long boutiqueId, HttpServletRequest request) {
        User user = getUserFromRequest(request);

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter la caisse !");
        }

        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

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

        Double montantCourant = calculerMontantCourantTheorique(caisse);
        dto.setMontantCourant(montantCourant);

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


    private Double calculerMontantCourantTheorique(Caisse caisse) {
        double montant = caisse.getMontantInitial() != null ? caisse.getMontantInitial() : 0.0;

        Long caisseId = caisse.getId();

        List<MouvementCaisse> ventes = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                caisseId, TypeMouvementCaisse.VENTE);
        montant += ventes.stream().mapToDouble(MouvementCaisse::getMontant).sum();

        List<MouvementCaisse> ajouts = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                caisseId, TypeMouvementCaisse.AJOUT);
        montant += ajouts.stream().mapToDouble(MouvementCaisse::getMontant).sum();

        List<MouvementCaisse> retraits = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                caisseId, TypeMouvementCaisse.RETRAIT);
        montant -= retraits.stream().mapToDouble(MouvementCaisse::getMontant).sum();

        List<MouvementCaisse> remboursements = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                caisseId, TypeMouvementCaisse.REMBOURSEMENT);
        montant -= remboursements.stream().mapToDouble(MouvementCaisse::getMontant).sum();

        return montant;
    }

  
    private Double calculerMontantDetteCaisse(Caisse caisse) {
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
        if (type == TypeMouvementCaisse.VENTE || type == TypeMouvementCaisse.AJOUT) {
                caisse.setMontantCourant(caisse.getMontantCourant() + montant);
            } else if (type == TypeMouvementCaisse.RETRAIT || type == TypeMouvementCaisse.REMBOURSEMENT) {
                caisse.setMontantCourant(caisse.getMontantCourant() - montant);
        }


        caisseRepository.save(caisse);
    }


    public List<CaisseResponseDTO> getCaissesByVendeur(Long vendeurId, HttpServletRequest request) {
    User user = utilitaire.getAuthenticatedUser(request);

    User vendeur = usersRepository.findById(vendeurId)
            .orElseThrow(() -> new RuntimeException("Vendeur introuvable"));

    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;

    if (!isAdminOrManager) {
        if (!user.getId().equals(vendeurId)) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les caisses de ce vendeur !");
        }
    }

    if (!vendeur.getEntreprise().getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès interdit : ce vendeur n'appartient pas à votre entreprise.");
    }

    List<Caisse> caisses = caisseRepository.findByVendeurId(vendeurId);

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

    @Transactional
    public CaisseResponseDTO enregistrerDepense(DepenseRequest request, HttpServletRequest httpRequest) {
        User user = getUserFromRequest(httpRequest);

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour effectuer une dépense !");
        }

        Boutique boutique = boutiqueRepository.findById(request.getBoutiqueId())
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));
        
        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

        Caisse caisse = caisseRepository.findByVendeurIdAndStatutAndBoutiqueId(
                user.getId(), StatutCaisse.OUVERTE, request.getBoutiqueId())
                .orElseThrow(() -> new RuntimeException("Aucune caisse ouverte pour ce vendeur dans cette boutique. Veuillez ouvrir une caisse avant de faire une dépense."));

        if (request.getMontant() > caisse.getMontantCourant()) {
            throw new RuntimeException("Montant insuffisant dans la caisse. Montant disponible: " + caisse.getMontantCourant() + ", Montant demandé: " + request.getMontant());
        }

        if (request.getMontant() <= 0) {
            throw new RuntimeException("Le montant de la dépense doit être positif.");
        }

        MouvementCaisse mouvement = new MouvementCaisse();
        mouvement.setCaisse(caisse);
        mouvement.setTypeMouvement(TypeMouvementCaisse.DEPENSE);
        mouvement.setMontant(request.getMontant());
        mouvement.setDateMouvement(LocalDateTime.now());
        mouvement.setDescription("Dépense: " + request.getMotif());
        mouvementCaisseRepository.save(mouvement);

        caisse.setMontantCourant(caisse.getMontantCourant() - request.getMontant());
        caisseRepository.save(caisse);

        return mapToCaisseResponseDTO(caisse);
    }

  
    public List<DepenseResponseDTO> listerDepensesCaisse(Long caisseId, HttpServletRequest request) {
        User user = getUserFromRequest(request);

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les dépenses !");
        }

        Caisse caisse = caisseRepository.findById(caisseId)
                .orElseThrow(() -> new RuntimeException("Caisse introuvable"));

        if (!caisse.getBoutique().getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette caisse n'appartient pas à votre entreprise.");
        }

        if (!isAdminOrManager && !caisse.getVendeur().getId().equals(user.getId())) {
            throw new RuntimeException("Vous n'êtes pas autorisé à consulter les dépenses de cette caisse.");
        }

        List<MouvementCaisse> mouvements = mouvementCaisseRepository.findByCaisseIdAndTypeMouvement(
                caisseId, TypeMouvementCaisse.DEPENSE);

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
     * Charge les dépenses pour plusieurs caisses en une requête (évite N+1 sur les endpoints paginés).
     * À utiliser quand on affiche une page de caisses : on passe les IDs de la page et on récupère
     * une map caisseId -> liste des dépenses.
     */
    public Map<Long, List<DepenseResponseDTO>> listerDepensesCaissesBatch(List<Long> caisseIds, HttpServletRequest request) { 
        Map<Long, List<DepenseResponseDTO>> result = new LinkedHashMap<>();
        if (caisseIds == null || caisseIds.isEmpty()) {
            return result;
        }
        User user = getUserFromRequest(request);
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            return result;
        }
        List<MouvementCaisse> mouvements = mouvementCaisseRepository.findByCaisseIdInAndTypeMouvementWithCaisseAndVendeurAndBoutique(
                caisseIds, TypeMouvementCaisse.DEPENSE);
        for (Long cid : caisseIds) {
            result.put(cid, new ArrayList<>());
        }
        for (MouvementCaisse mouvement : mouvements) {
            Long cid = mouvement.getCaisse().getId();
            DepenseResponseDTO dto = new DepenseResponseDTO();
            dto.setId(mouvement.getId());
            dto.setCaisseId(cid);
            dto.setMontant(mouvement.getMontant());
            dto.setDescription(mouvement.getDescription());
            dto.setDateMouvement(mouvement.getDateMouvement());
            dto.setNomVendeur(mouvement.getCaisse().getVendeur() != null ? mouvement.getCaisse().getVendeur().getNomComplet() : null);
            dto.setNomBoutique(mouvement.getCaisse().getBoutique() != null ? mouvement.getCaisse().getBoutique().getNomBoutique() : null);
            result.get(cid).add(dto);
        }
        return result;
    }

  
    public List<DepenseResponseDTO> listerDepensesVendeur(Long boutiqueId, HttpServletRequest request) {
        User user = getUserFromRequest(request);

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.VENDRE_PRODUITS);
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les dépenses !");
        }

        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("Boutique introuvable"));

        if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette boutique n'appartient pas à votre entreprise.");
        }

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