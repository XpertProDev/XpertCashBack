package com.xpertcash.service;

import com.xpertcash.DTOs.TransfertFondsRequestDTO;
import com.xpertcash.DTOs.TransfertFondsResponseDTO;
import com.xpertcash.DTOs.TresorerieDTO;
import com.xpertcash.entity.*;
import com.xpertcash.entity.Enum.Ordonnateur;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.SourceDepense;
import com.xpertcash.entity.Enum.SourceTresorerie;
import com.xpertcash.entity.Enum.TypeCharge;
import com.xpertcash.entity.VENTE.*;
import com.xpertcash.exceptions.BusinessException;
import com.xpertcash.repository.*;
import com.xpertcash.repository.VENTE.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransfertFondsService {

    private static final Logger logger = LoggerFactory.getLogger(TransfertFondsService.class);

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private TransfertFondsRepository transfertFondsRepository;

    @Autowired
    private DepenseGeneraleRepository depenseGeneraleRepository;

    @Autowired
    private CaisseRepository caisseRepository;

    @Autowired
    private MouvementCaisseRepository mouvementCaisseRepository;

    @Autowired
    private TresorerieService tresorerieService;

    @Transactional
    public TransfertFondsResponseDTO effectuerTransfert(TransfertFondsRequestDTO request, HttpServletRequest httpRequest) {
        User user = validerUtilisateur(httpRequest);
        validerPermissions(user);
        validerRequete(request);

        SourceTresorerie source = parseSourceTresorerie(request.getSource());
        SourceTresorerie destination = parseSourceTresorerie(request.getDestination());

        if (source == destination) {
            throw new BusinessException("La source et la destination doivent être différentes.");
        }

        validerCaissesFermees(user.getEntreprise().getId(), source, destination);
        validerMontantDisponible(user.getEntreprise().getId(), source, request.getMontant());

        TransfertFonds transfert = creerTransfert(request, user, source, destination);
        transfert = transfertFondsRepository.save(transfert);

        enregistrerMouvements(user.getEntreprise().getId(), source, destination, request.getMontant(), request.getMotif(), user);

        logger.info("Transfert de fonds effectué : {} {} -> {} {} par utilisateur {}", 
                request.getMontant(), source, destination, request.getMontant(), user.getId());

        return mapperVersResponseDTO(transfert);
    }

    @Transactional(readOnly = true)
    public List<TransfertFondsResponseDTO> listerTransferts(HttpServletRequest httpRequest) {
        User user = validerUtilisateur(httpRequest);
        validerPermissions(user);
        
        List<TransfertFonds> transferts = transfertFondsRepository.findByEntrepriseIdOrderByDateTransfertDesc(user.getEntreprise().getId());
        return transferts.stream()
                .map(this::mapperVersResponseDTO)
                .collect(Collectors.toList());
    }

    private User validerUtilisateur(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        if (user.getEntreprise() == null) {
            throw new BusinessException("Vous n'êtes associé à aucune entreprise.");
        }
        return user;
    }

    private void validerPermissions(User user) {
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(com.xpertcash.entity.PermissionType.COMPTABILITE);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new BusinessException("Vous n'avez pas les droits nécessaires pour effectuer des transferts de fonds.");
        }
    }

    private void validerRequete(TransfertFondsRequestDTO request) {
        if (request.getMontant() == null || request.getMontant() <= 0) {
            throw new BusinessException("Le montant doit être supérieur à zéro.");
        }
        if (request.getSource() == null || request.getSource().trim().isEmpty()) {
            throw new BusinessException("La source est obligatoire.");
        }
        if (request.getDestination() == null || request.getDestination().trim().isEmpty()) {
            throw new BusinessException("La destination est obligatoire.");
        }
        if (request.getMotif() == null || request.getMotif().trim().isEmpty()) {
            throw new BusinessException("Le motif est obligatoire.");
        }
        if (request.getMotif().length() > 500) {
            throw new BusinessException("Le motif ne peut pas dépasser 500 caractères.");
        }
        if (request.getPersonneALivrer() == null || request.getPersonneALivrer().trim().isEmpty()) {
            throw new BusinessException("La personne à livrer est obligatoire.");
        }
        if (request.getPersonneALivrer().length() > 500) {
            throw new BusinessException("La personne à livrer ne peut pas dépasser 500 caractères.");
        }
    }

    private SourceTresorerie parseSourceTresorerie(String source) {
        try {
            return SourceTresorerie.valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Source invalide : " + source + ". Valeurs acceptées : CAISSE, BANQUE, MOBILE_MONEY");
        }
    }

    private void validerCaissesFermees(Long entrepriseId, SourceTresorerie source, SourceTresorerie destination) {
        if (source != SourceTresorerie.CAISSE && destination != SourceTresorerie.CAISSE) {
            return;
        }

        List<Caisse> caissesFermees = caisseRepository.findByEntrepriseIdAndStatut(entrepriseId, StatutCaisse.FERMEE);

        if (caissesFermees.isEmpty()) {
            throw new BusinessException("Aucune caisse fermée trouvée. Les transferts depuis/vers la caisse ne peuvent être effectués qu'avec des caisses fermées.");
        }
    }

    private void validerMontantDisponible(Long entrepriseId, SourceTresorerie source, Double montant) {
        TresorerieDTO tresorerie = tresorerieService.calculerTresorerieParEntrepriseId(entrepriseId);
        
        double montantDisponible = 0.0;
        switch (source) {
            case CAISSE:
                montantDisponible = tresorerie.getMontantCaisse() != null ? tresorerie.getMontantCaisse() : 0.0;
                break;
            case BANQUE:
                montantDisponible = tresorerie.getMontantBanque() != null ? tresorerie.getMontantBanque() : 0.0;
                break;
            case MOBILE_MONEY:
                montantDisponible = tresorerie.getMontantMobileMoney() != null ? tresorerie.getMontantMobileMoney() : 0.0;
                break;
        }

        if (montantDisponible < montant) {
            throw new BusinessException("Montant insuffisant dans " + source.name() + ". Montant disponible : " + montantDisponible);
        }
    }

    private TransfertFonds creerTransfert(TransfertFondsRequestDTO request, User user, SourceTresorerie source, SourceTresorerie destination) {
        TransfertFonds transfert = new TransfertFonds();
        transfert.setMontant(request.getMontant());
        transfert.setSource(source);
        transfert.setDestination(destination);
        transfert.setMotif(request.getMotif().trim());
        transfert.setPersonneALivrer(request.getPersonneALivrer().trim());
        transfert.setEntreprise(user.getEntreprise());
        transfert.setCreePar(user);
        return transfert;
    }

    private void enregistrerMouvements(Long entrepriseId, SourceTresorerie source, SourceTresorerie destination,
                                      Double montant, String motif, User user) {
        String descriptionSortie = "Transfert vers " + destination.name() + " - " + motif;
        String descriptionEntree = "Transfert depuis " + source.name() + " - " + motif;

        SourceDepense sourceDepenseSortie = convertirVersSourceDepense(source);
        SourceDepense sourceDepenseEntree = convertirVersSourceDepense(destination);

        DepenseGenerale depenseSortie = creerDepenseGenerale(entrepriseId, montant, sourceDepenseSortie, descriptionSortie, user);
        DepenseGenerale depenseEntree = creerDepenseGenerale(entrepriseId, -montant, sourceDepenseEntree, descriptionEntree, user);

        depenseGeneraleRepository.save(depenseSortie);
        depenseGeneraleRepository.save(depenseEntree);

        if (source == SourceTresorerie.CAISSE) {
            enregistrerMouvementCaisse(entrepriseId, TypeMouvementCaisse.RETRAIT, montant, descriptionSortie);
        }
        if (destination == SourceTresorerie.CAISSE) {
            enregistrerMouvementCaisse(entrepriseId, TypeMouvementCaisse.AJOUT, montant, descriptionEntree);
        }
    }

    private SourceDepense convertirVersSourceDepense(SourceTresorerie source) {
        switch (source) {
            case CAISSE:
                return SourceDepense.CAISSE;
            case BANQUE:
                return SourceDepense.BANQUE;
            case MOBILE_MONEY:
                return SourceDepense.MOBILE_MONEY;
            default:
                throw new BusinessException("Source non supportée : " + source);
        }
    }

    private DepenseGenerale creerDepenseGenerale(Long entrepriseId, Double montant, SourceDepense source,
                                                 String description, User user) {
        DepenseGenerale depense = new DepenseGenerale();
        depense.setDesignation(description);
        depense.setPrixUnitaire(Math.abs(montant));
        depense.setQuantite(montant < 0 ? -1 : 1);
        depense.setMontant(montant);
        depense.setSource(source);
        depense.setOrdonnateur(Ordonnateur.MANAGER);
        depense.setTypeCharge(TypeCharge.CHARGE_FIXE);
        depense.setEntreprise(user.getEntreprise());
        depense.setCreePar(user);
        depense.setNumero(null);
        return depense;
    }

    private void enregistrerMouvementCaisse(Long entrepriseId, TypeMouvementCaisse type, Double montant, String description) {
        List<Caisse> caissesFermees = caisseRepository.findByEntrepriseIdAndStatutOrderByDateFermetureDesc(
                entrepriseId, StatutCaisse.FERMEE);

        if (!caissesFermees.isEmpty()) {
            Caisse caisse = caissesFermees.get(0);
            MouvementCaisse mouvement = new MouvementCaisse();
            mouvement.setCaisse(caisse);
            mouvement.setTypeMouvement(type);
            mouvement.setMontant(montant);
            mouvement.setDateMouvement(java.time.LocalDateTime.now());
            mouvement.setDescription(description);
            mouvement.setModePaiement(ModePaiement.ESPECES);

            if (type == TypeMouvementCaisse.AJOUT) {
                caisse.setMontantCourant(caisse.getMontantCourant() + montant);
            } else if (type == TypeMouvementCaisse.RETRAIT) {
                caisse.setMontantCourant(caisse.getMontantCourant() - montant);
            }

            mouvementCaisseRepository.save(mouvement);
            caisseRepository.save(caisse);
        }
    }

    private TransfertFondsResponseDTO mapperVersResponseDTO(TransfertFonds transfert) {
        TransfertFondsResponseDTO dto = new TransfertFondsResponseDTO();
        dto.setId(transfert.getId());
        dto.setDateTransfert(transfert.getDateTransfert());
        dto.setMotif(transfert.getMotif());
        dto.setResponsable(transfert.getCreePar().getNomComplet());
        dto.setDe(transfert.getSource().name());
        dto.setVers(transfert.getDestination().name());
        dto.setMontant(transfert.getMontant());
        dto.setPersonneALivrer(transfert.getPersonneALivrer());
        dto.setEntrepriseId(transfert.getEntreprise().getId());
        dto.setEntrepriseNom(transfert.getEntreprise().getNomEntreprise());
        dto.setTypeTransaction("TRANSFERT");
        return dto;
    }
}
