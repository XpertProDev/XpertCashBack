package com.xpertcash.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Comparator;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.xpertcash.DTOs.EntrepriseClientDTO;
import com.xpertcash.DTOs.FactureReelleDTO;
import com.xpertcash.DTOs.LigneFactureDTO;
import com.xpertcash.DTOs.PaiementDTO;
import com.xpertcash.DTOs.PaginatedResponseDTO;
import com.xpertcash.DTOs.StatistiquesFactureReelleDTO;
import com.xpertcash.DTOs.TopClientFactureDTO;
import com.xpertcash.DTOs.TopCreateurFactureDTO;
import com.xpertcash.DTOs.CLIENT.ClientDTO;
import com.xpertcash.composant.Utilitaire;
import com.xpertcash.configuration.CentralAccess;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.EntreeGenerale;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.LigneFactureReelle;
import com.xpertcash.entity.ModePaiement;
import com.xpertcash.entity.Paiement;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Enum.SourceDepense;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.StatutFactureProForma;
import com.xpertcash.entity.Enum.StatutPaiementFacture;
import com.xpertcash.repository.EntreeGeneraleRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.LigneFactureReelleRepository;
import com.xpertcash.repository.PaiementRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.Module.ModuleActivationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class FactureReelleService {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private FactureReelleRepository factureReelleRepository;

    @Autowired
    private LigneFactureReelleRepository ligneFactureReelleRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private EntreeGeneraleRepository entreeGeneraleRepository;

     @Autowired
    private FactProHistoriqueService factProHistoriqueService;
    @Autowired
    private FactureProformaRepository factureProformaRepository;

    @Autowired
    private Utilitaire utilitaire;

    @Autowired
    private ModuleActivationService moduleActivationService;




    public FactureReelle genererFactureReelle(FactureProForma factureProForma) {
        FactureReelle factureReelle = new FactureReelle();

        // Copier les informations de la proforma
        factureReelle.setNumeroFacture(genererNumeroFactureReel(factureProForma.getEntreprise()));
        factureReelle.setDateCreation(LocalDate.now());
        factureReelle.setDateCreationPro(factureProForma.getDateCreation());
        factureReelle.setTotalHT(factureProForma.getTotalHT());
        factureReelle.setRemise(factureProForma.getRemise());
        // Arrondir le taux de remise à 2 décimales (ex: 2,96 % au lieu de 2,958579881656805)
        Double tauxRemise = factureProForma.getTauxRemise();
        factureReelle.setTauxRemise(tauxRemise != null ? Math.round(tauxRemise * 100.0) / 100.0 : null);
        factureReelle.setDescription(factureProForma.getDescription());

        factureReelle.setTva(factureProForma.isTva());
        factureReelle.setTotalFacture(factureProForma.getTotalFacture());
        factureReelle.setStatutPaiement(StatutPaiementFacture.EN_ATTENTE);
        factureReelle.setUtilisateurCreateur(factureProForma.getUtilisateurCreateur());
        factureReelle.setUtilisateurValidateur(factureProForma.getUtilisateurValidateur());
        factureReelle.setClient(factureProForma.getClient());
        factureReelle.setEntrepriseClient(factureProForma.getEntrepriseClient());
        factureReelle.setEntreprise(factureProForma.getEntreprise());
        factureReelle.setFactureProForma(factureProForma);

        // save la facture réelle AVANT d'ajouter les lignes
        FactureReelle factureReelleSauvegardee = factureReelleRepository.save(factureReelle);

        // Copier les lignes de facture
        List<LigneFactureReelle> lignesFacture = factureProForma.getLignesFacture().stream().map(ligneProForma -> {
            LigneFactureReelle ligneReelle = new LigneFactureReelle();
            ligneReelle.setProduit(ligneProForma.getProduit());
            ligneReelle.setQuantite(ligneProForma.getQuantite());
            ligneReelle.setPrixUnitaire(ligneProForma.getPrixUnitaire());
            ligneReelle.setLigneDescription(ligneProForma.getLigneDescription());
            ligneReelle.setMontantTotal(ligneProForma.getMontantTotal());
            ligneReelle.setFactureReelle(factureReelleSauvegardee);
            return ligneReelle;
        }).collect(Collectors.toList());

        ligneFactureReelleRepository.saveAll(lignesFacture);

        return factureReelleSauvegardee;
    }


    private String genererNumeroFactureReel(Entreprise entreprise) {
        LocalDate currentDate = LocalDate.now();
        int year = currentDate.getYear();
        String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("MM-yyyy"));

        List<FactureReelle> facturesDeLAnnee = factureReelleRepository.findFacturesDeLAnneeParEntreprise(entreprise.getId(), year);
        long newIndex = 1;

        if (!facturesDeLAnnee.isEmpty()) {
            String lastNumeroFacture = facturesDeLAnnee.get(0).getNumeroFacture();

            Pattern pattern = Pattern.compile("(\\d+)");
            Matcher matcher = pattern.matcher(lastNumeroFacture);

            if (matcher.find()) {
                try {
                    newIndex = Long.parseLong(matcher.group(1)) + 1;
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Impossible de parser l'index numérique dans le numéro : " + lastNumeroFacture, e);
                }
            } else {
                throw new RuntimeException("Format de numéro de facture invalide : " + lastNumeroFacture);
            }
        }

        String indexFormate = String.format("%03d", newIndex);

        String prefixe = entreprise != null && entreprise.getPrefixe() != null ? entreprise.getPrefixe().trim() : "";
        String suffixe = entreprise != null && entreprise.getSuffixe() != null ? entreprise.getSuffixe().trim() : "";

        StringBuilder numeroFacture = new StringBuilder("");

        if (!prefixe.isEmpty() && suffixe.isEmpty()) {
            numeroFacture.append(prefixe).append("-").append(indexFormate).append("-").append(formattedDate);
        } else if (prefixe.isEmpty() && !suffixe.isEmpty()) {
            numeroFacture.append(indexFormate).append("-").append(formattedDate).append("-").append(suffixe);
        } else if (!prefixe.isEmpty() && !suffixe.isEmpty()) {
            numeroFacture.append(prefixe).append("-").append(indexFormate).append("-").append(formattedDate);
        } else {
            numeroFacture.append(indexFormate).append("-").append(formattedDate);
        }

        return numeroFacture.toString();
    }

   // Methode pour Supprimer facturer deja generer une fois annuler
    public void supprimerFactureReelleLiee(FactureProForma proforma) {
        Entreprise entreprise = proforma.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("La facture proforma n'est associée à aucune entreprise.");
        }
        
        List<FactureReelle> facturesReelles = factureReelleRepository.findAllByFactureProFormaIdAndEntrepriseId(
                proforma.getId(), entreprise.getId());
        if (facturesReelles.isEmpty()) {
            System.out.println("Aucune facture réelle associée à cette facture proforma.");
            return;
        }

        for (FactureReelle factureReelle : facturesReelles) {
            factureReelleRepository.delete(factureReelle);
            System.out.println(" Facture réelle supprimée suite à l'annulation.");
        }
    }


   // Méthode pour lister les factures réelles (ancienne version pour compatibilité)
  public List<FactureReelleDTO> listerMesFacturesReelles(HttpServletRequest request) {
    PaginatedResponseDTO<FactureReelleDTO> result = listerMesFacturesReellesPaginated(0, 50, null, request);
    return result.getContent();
  }

  /**
   * Méthode scalable avec pagination pour lister les factures réelles.
   * search : si non vide (≥ 2 caractères), filtre par numéro de facture ou nom du client — côté base.
   */
  public PaginatedResponseDTO<FactureReelleDTO> listerMesFacturesReellesPaginated(int page, int size, String search, HttpServletRequest request) {
    if (page < 0) page = 0;
    if (size <= 0) size = 20;
    if (size > 100) size = 100;

    User utilisateur = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entreprise = utilisateur.getEntreprise();
    if (entreprise == null) {
        throw new SecurityException("L'utilisateur n'est associé à aucune entreprise");
    }

    boolean isAdminOrManager = CentralAccess.isSelfOrAdminOrManager(utilisateur, entreprise.getId());
    boolean hasPermission = utilisateur.getRole().hasPermission(PermissionType.COMPTABILITE);

    try {
        moduleActivationService.verifierAccesModulePourEntreprise(entreprise, "GESTION_FACTURATION");
    } catch (Exception e) {
        throw new SecurityException("Le module de gestion des factures n'est pas activé pour cette entreprise", e);
    }

    if (!(isAdminOrManager || hasPermission)) {
        throw new SecurityException("Accès interdit : Vous n'avez pas les permissions nécessaires pour voir ces factures.");
    }

    String searchTrimmed = (search != null) ? search.trim() : "";
    if (searchTrimmed.length() < 2) {
        searchTrimmed = "";
    }

    Pageable pageable = PageRequest.of(page, size,
        Sort.by("dateCreation").descending().and(Sort.by("id").descending()));

    Page<FactureReelle> facturesPage = searchTrimmed.isEmpty()
            ? factureReelleRepository.findByEntrepriseOrderByDateCreationDescPaginatedWithRelations(entreprise.getId(), pageable)
            : factureReelleRepository.findByEntrepriseOrderByDateCreationDescPaginatedWithRelationsAndSearch(entreprise.getId(), searchTrimmed, pageable);

    List<Long> factureIds = facturesPage.getContent().stream()
            .map(FactureReelle::getId)
            .collect(Collectors.toList());

    Map<Long, BigDecimal> paiementsMap = paiementRepository.sumMontantsByFactureReelleIds(factureIds)
            .stream()
            .collect(Collectors.toMap(
                obj -> (Long) obj[0],
                obj -> (BigDecimal) obj[1]
            ));

    // Chargement en batch des lignes pour éviter N+1
    Map<Long, List<LigneFactureReelle>> lignesByFactureId = factureIds.isEmpty() ? Collections.emptyMap()
            : ligneFactureReelleRepository.findByFactureReelleIdIn(factureIds).stream()
                    .collect(Collectors.groupingBy(l -> l.getFactureReelle().getId()));

    facturesPage.getContent().forEach(f -> f.setLignesFacture(
            lignesByFactureId.getOrDefault(f.getId(), Collections.emptyList())));

    Page<FactureReelleDTO> facturesDTO = facturesPage.map(facture -> {
        BigDecimal totalFacture = BigDecimal.valueOf(facture.getTotalFacture());
        BigDecimal totalPaye = paiementsMap.getOrDefault(facture.getId(), BigDecimal.ZERO);
        BigDecimal montantRestant = totalFacture.subtract(totalPaye);
        return new FactureReelleDTO(facture, montantRestant);
    });

    return PaginatedResponseDTO.fromPage(facturesDTO);
}
 
    // Trier les facture par mois/année
   public ResponseEntity<?> filtrerFacturesParMoisEtAnnee(Integer mois, Integer annee, HttpServletRequest request) {
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Long entrepriseId = user.getEntreprise().getId();
    if (user.getEntreprise() == null) {
        throw new SecurityException("Utilisateur non associé à une entreprise");
    }

    boolean isAdminOrManager = CentralAccess.isSelfOrAdminOrManager(user, entrepriseId);
    boolean hasPermission = user.getRole().hasPermission(PermissionType.COMPTABILITE);

    if (!(isAdminOrManager || hasPermission)) {
        throw new SecurityException("Accès interdit : Vous n'avez pas les permissions nécessaires pour filtrer les factures.");
    }
  

    List<FactureReelle> factures;

    if (mois != null && annee != null) {
        factures = factureReelleRepository.findByMonthAndYearAndEntreprise(mois, annee, entrepriseId);
    } else if (mois != null) {
        factures = factureReelleRepository.findByMonthAndEntreprise(mois, entrepriseId);
    } else if (annee != null) {
        factures = factureReelleRepository.findByYearAndEntreprise(annee, entrepriseId);
    } else {
        factures = factureReelleRepository.findByEntrepriseId(entrepriseId);
    }

    List<Long> factureIds = factures.stream()
            .map(FactureReelle::getId)
            .collect(Collectors.toList());
    
    Map<Long, BigDecimal> paiementsMap = paiementRepository.sumMontantsByFactureReelleIds(factureIds)
            .stream()
            .collect(Collectors.toMap(
                obj -> (Long) obj[0],
                obj -> (BigDecimal) obj[1]
            ));

    List<FactureReelleDTO> factureDTOs = factures.stream().map(facture -> {
        BigDecimal totalFacture = BigDecimal.valueOf(facture.getTotalFacture());
        BigDecimal totalPaye = paiementsMap.getOrDefault(facture.getId(), BigDecimal.ZERO);
        BigDecimal montantRestant = totalFacture.subtract(totalPaye);
        
        FactureReelleDTO dto = new FactureReelleDTO(facture, montantRestant);

        dto.setEntrepriseClient(null);
        dto.setClient(null);
        dto.setLignesFacture(null);

        if (facture.getClient() != null) {
            dto.setNomClient(facture.getClient().getNomComplet());
        } else if (facture.getEntrepriseClient() != null) {
            dto.setNomEntrepriseClient(facture.getEntrepriseClient().getNom());
        }

        return dto;
    }).collect(Collectors.toList());

    if (factureDTOs.isEmpty()) {
        return ResponseEntity.ok("Aucune facture trouvée.");
    }

    return ResponseEntity.ok(factureDTOs);
}

    public FactureReelleDTO getFactureReelleById(Long factureId, HttpServletRequest request) {
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    FactureReelle facture = factureReelleRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Aucune facture trouvée"));

    Hibernate.initialize(facture.getFactureProForma());

    Long entrepriseFactureId = facture.getEntreprise() != null ? facture.getEntreprise().getId() : null;
    Long entrepriseUserId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;

    if (entrepriseFactureId == null || entrepriseUserId == null || !entrepriseFactureId.equals(entrepriseUserId)) {
        throw new RuntimeException("Accès refusé : cette facture ne vous appartient pas !");
    }

    boolean isAdminOrManagerOfEntreprise = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseFactureId);
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);
    boolean isCreateur = facture.getUtilisateurCreateur().getId().equals(user.getId());

    if (!(isAdminOrManagerOfEntreprise || hasPermission || isCreateur)) {
        throw new RuntimeException("Accès interdit : vous n'avez pas les droits pour consulter cette facture !");
    }


    BigDecimal totalFacture = BigDecimal.valueOf(facture.getTotalFacture());
    BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(factureId);
    if (totalPaye == null) totalPaye = BigDecimal.ZERO;

    BigDecimal montantRestant = totalFacture.subtract(totalPaye);

    return new FactureReelleDTO(facture, montantRestant);
}


  public FactureReelle enregistrerPaiement(Long factureId, BigDecimal montant, String modePaiement, HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User utilisateur = authHelper.getAuthenticatedUserWithFallback(request);

    FactureReelle facture = factureReelleRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Facture introuvable"));

    Entreprise entrepriseUtilisateur = utilisateur.getEntreprise();
    Entreprise entrepriseFacture = facture.getEntreprise();

    if (entrepriseUtilisateur == null || entrepriseFacture == null ||
        !entrepriseUtilisateur.getId().equals(entrepriseFacture.getId())) {
        throw new RuntimeException("L'utilisateur et la facture ne sont pas dans la même entreprise.");
    }

    boolean isAdmin = CentralAccess.isAdminOfEntreprise(utilisateur, entrepriseUtilisateur.getId());
    boolean hasPermission = utilisateur.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

    if (!isAdmin && !hasPermission) {
        throw new RuntimeException("Vous n'avez pas les droits pour enregistrer un paiement.");
    }

    moduleActivationService.verifierAccesModulePourEntreprise(entrepriseUtilisateur, "GESTION_FACTURATION");

    if (facture.getStatutPaiement() == StatutPaiementFacture.PAYEE) {
        throw new RuntimeException("Cette facture est déjà totalement réglée.");
    }

    BigDecimal totalPayeAvant = paiementRepository.sumMontantsByFactureReelle(factureId);
    if (totalPayeAvant == null) totalPayeAvant = BigDecimal.ZERO;

    BigDecimal totalFacture = BigDecimal.valueOf(facture.getTotalFacture());
    BigDecimal totalApresPaiement = totalPayeAvant.add(montant);

    if (totalApresPaiement.compareTo(totalFacture) > 0) {
        BigDecimal montantRestant = totalFacture.subtract(totalPayeAvant);
        throw new RuntimeException("Le paiement dépasse le montant total de la facture. Montant restant dû : " + montantRestant + " FCFA");
    }

    Paiement paiement = new Paiement();
    paiement.setMontant(montant);
    paiement.setDatePaiement(LocalDateTime.now());
    paiement.setFactureReelle(facture);
    String modePaiementNormalise = normaliserModePaiementPourStockage(modePaiement);
    paiement.setModePaiement(modePaiementNormalise);
    paiement.setEncaissePar(utilisateur);

    paiementRepository.save(paiement);


    creerEntreeGeneralePourPaiement(paiement, facture, utilisateur);

    BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(factureId);
    if (totalPaye.compareTo(totalFacture) >= 0) {
        facture.setStatutPaiement(StatutPaiementFacture.PAYEE);
    } else if (totalPaye.compareTo(BigDecimal.ZERO) > 0) {
        facture.setStatutPaiement(StatutPaiementFacture.PARTIELLEMENT_PAYEE);
    } else {
        facture.setStatutPaiement(StatutPaiementFacture.EN_ATTENTE);
    }

    return factureReelleRepository.save(facture);
}

    //Facture impayer dune facture
    public BigDecimal getMontantRestant(Long factureId) {
        FactureReelle facture = factureReelleRepository.findById(factureId)
            .orElseThrow(() -> new RuntimeException("Facture introuvable"));

        BigDecimal totalFacture = BigDecimal.valueOf(facture.getTotalFacture());
        BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(factureId);

        return totalFacture.subtract(totalPaye);
    }

    //Get les paiements d'une facture
  public List<PaiementDTO> getPaiementsParFacture(Long factureId, HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Utilisateur n'est associé à aucune entreprise");
    }

    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasGestionFacturePermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

    FactureReelle facture = factureReelleRepository.findById(factureId)
        .orElseThrow(() -> new RuntimeException("Facture introuvable"));

    if (!facture.getEntreprise().getId().equals(entreprise.getId())) {
        throw new RuntimeException("Accès interdit à une facture d'une autre entreprise");
    }

    boolean isCreateur = facture.getUtilisateurCreateur().getId().equals(user.getId());
    if (!(isAdminOrManager || hasGestionFacturePermission || isCreateur)) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour consulter les paiements de cette facture");
    }

    List<Paiement> paiements = paiementRepository.findByFactureReelle(facture);
    return paiements.stream().map(PaiementDTO::new).collect(Collectors.toList());
}


    //Facture impayer all facture
   public List<FactureReelleDTO> listerFacturesImpayees(HttpServletRequest request) {
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    if (user.getEntreprise() == null) {
        throw new RuntimeException("Utilisateur n'est associé à aucune entreprise");
    }

    Long entrepriseId = user.getEntreprise().getId();

    boolean isAuthorized = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseId)
            || user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

    List<StatutPaiementFacture> statutsImpayes = List.of(
        StatutPaiementFacture.EN_ATTENTE,
        StatutPaiementFacture.PARTIELLEMENT_PAYEE
    );

    List<FactureReelle> factures;

    if (isAuthorized) {
        factures = factureReelleRepository.findByEntrepriseIdAndStatutPaiementIn(entrepriseId, statutsImpayes);
    } else {
        factures = factureReelleRepository.findByEntrepriseIdAndUtilisateurCreateurIdAndStatutPaiementIn(
            entrepriseId, user.getId(), statutsImpayes
        );
    }

    return factures.stream().map(facture -> {
        BigDecimal total = BigDecimal.valueOf(facture.getTotalFacture());
        BigDecimal paye = paiementRepository.sumMontantsByFactureReelle(facture.getId());
        if (paye == null) paye = BigDecimal.ZERO;

        BigDecimal montantRestant = total.subtract(paye);

        FactureReelleDTO dto = new FactureReelleDTO(facture, montantRestant);

        dto.setEntrepriseClient(null);
        dto.setClient(null);
        dto.setLignesFacture(null);

        return dto;
    }).collect(Collectors.toList());
}


//Modifier le statut d'une facture
@Transactional
public FactureProForma annulerFactureReelle(FactureReelle modifications, HttpServletRequest request) {
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    FactureReelle factureReelle = factureReelleRepository.findById(modifications.getId())
            .orElseThrow(() -> new RuntimeException("Facture réelle introuvable !"));

    FactureProForma factureProForma = factureReelle.getFactureProForma();
    Long entrepriseFactureId = factureReelle.getEntreprise().getId();

    boolean isAuthorized = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseFactureId)
            || user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);

    if (!isAuthorized) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour annuler cette facture.");
    }

    if (factureProForma.getStatut() == StatutFactureProForma.ANNULE) {
        throw new RuntimeException("Cette facture est déjà annulée.");
    }

    BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(factureReelle.getId());
    if (totalPaye != null && totalPaye.compareTo(BigDecimal.ZERO) > 0) {
        throw new RuntimeException("Impossible d’annuler : des paiements ont déjà été effectués.");
    }

    supprimerFactureReelleLiee(factureProForma);

    factureProForma.setStatut(StatutFactureProForma.ANNULE);
    factureProForma.setDateAnnulation(LocalDateTime.now());
    factureProForma.setUtilisateurAnnulateur(user);
    factureProForma.setDateRelance(null);
    factureProForma.setDernierRappelEnvoye(null);
    factureProForma.setNotifie(false);

    factProHistoriqueService.enregistrerActionHistorique(
            factureProForma,
            user,
            "Annulation",
            "Facture réelle supprimée et proforma annulée. Aucun paiement enregistré."
    );

    return factureProformaRepository.save(factureProForma);
}



//Trier
public List<FactureReelleDTO> getFacturesParPeriode(Long userIdRequete, HttpServletRequest request,
                                                     String typePeriode, LocalDate dateDebut, LocalDate dateFin) {
    User currentUser = authHelper.getAuthenticatedUserWithFallback(request);
    User targetUser = usersRepository.findById(userIdRequete)
            .orElseThrow(() -> new RuntimeException("Utilisateur cible non trouvé"));

    Entreprise entrepriseCourante = currentUser.getEntreprise();
    Entreprise entrepriseCible = targetUser.getEntreprise();

    if (entrepriseCourante == null || entrepriseCible == null
        || !entrepriseCourante.getId().equals(entrepriseCible.getId())) {
        throw new RuntimeException("Opération interdite : utilisateurs de différentes entreprises.");
    }

    LocalDateTime dateStart;
    LocalDateTime dateEnd;

    switch (typePeriode.toLowerCase()) {
        case "jour":
            dateStart = LocalDate.now().atStartOfDay();
            dateEnd = dateStart.plusDays(1);
            break;
        case "mois":
            dateStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
            dateEnd = dateStart.plusMonths(1);
            break;
        case "annee":
            dateStart = LocalDate.now().withDayOfYear(1).atStartOfDay();
            dateEnd = dateStart.plusYears(1);
            break;
        case "personnalise":
            if (dateDebut == null || dateFin == null) {
                throw new RuntimeException("Dates de début et de fin requises pour une période personnalisée.");
            }
            dateStart = dateDebut.atStartOfDay();
            dateEnd = dateFin.plusDays(1).atStartOfDay();
            break;
        default:
            throw new RuntimeException("Type de période invalide.");
    }

    List<FactureReelle> factures = factureReelleRepository.findByEntrepriseIdAndDateCreationBetween(
            entrepriseCourante.getId(), dateStart.toLocalDate(), dateEnd.toLocalDate()
    );


    return factures.stream()
        .sorted(Comparator.comparing(FactureReelle::getDateCreation).reversed())
        .map(facture -> {
            FactureReelleDTO factureDTO = new FactureReelleDTO();

            factureDTO.setId(facture.getId());
            factureDTO.setNumeroFacture(facture.getNumeroFacture());
            factureDTO.setDateCreation(facture.getDateCreation());
            factureDTO.setDescription(facture.getDescription());
            factureDTO.setTotalHT(facture.getTotalHT());
            factureDTO.setRemise(Math.round(facture.getRemise() * 100.0) / 100.0);
            factureDTO.setTva(facture.isTva());
            factureDTO.setTotalFacture(facture.getTotalFacture());

            if (facture.getLignesFacture() != null && !facture.getLignesFacture().isEmpty()) {
                System.out.println("Lignes de facture pour la facture " + facture.getNumeroFacture() + ": " + facture.getLignesFacture());
            } else {
                System.out.println("Aucune ligne de facture pour la facture " + facture.getNumeroFacture());
            }

            factureDTO.setLigneFactureProforma(facture.getLignesFacture() != null && !facture.getLignesFacture().isEmpty() ?
                facture.getLignesFacture().stream()
                    .map(LigneFactureDTO::new)
                    .collect(Collectors.toList()) : null);

            factureDTO.setClient(facture.getClient() != null ? new ClientDTO(facture.getClient()) : null);

            factureDTO.setEntrepriseClient(facture.getEntrepriseClient() != null ? new EntrepriseClientDTO(facture.getEntrepriseClient()) : null);

            return factureDTO;
        })
        .collect(Collectors.toList());
}

    // Récupère les factures réelles récentes de l'entreprise
    public List<FactureReelleDTO> getFacturesReellesRecentes(int limit, HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        if (user.getEntreprise() == null) {
            throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
        }

        Long entrepriseId = user.getEntreprise().getId();

        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à cette information.");
        }

        List<FactureReelle> factures = factureReelleRepository.findRecentFacturesReellesByEntrepriseId(entrepriseId);

        return factures.stream()
                .limit(limit)
                .map(facture -> {
                    BigDecimal montantRestant = getMontantRestant(facture.getId());
                    return new FactureReelleDTO(facture, montantRestant);
                })
                .collect(Collectors.toList());
    }

    // Stats globales des factures réelles (filtre par période)
    public StatistiquesFactureReelleDTO getStatistiquesGlobales(String periode, HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Long entrepriseId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;
        if (entrepriseId == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        // Vérification des droits adamin manager ou comptable ou gestionnaire de facturation
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseId);
        boolean isComptable = CentralAccess.isComptable(user, entrepriseId);
        boolean hasGestionFacturePermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);
        if (!isAdminOrManager && !isComptable && !hasGestionFacturePermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour consulter les statistiques globales des factures.");
        }

        PeriodeDates periodeDates = calculerDatesPeriodeStatistiques(periode);
        LocalDate dateDebut = periodeDates.dateDebut;
        LocalDate dateFin = periodeDates.dateFin;

        // Stats par statut (sert aussi à calculer les totaux globaux)
        List<Object[]> statsParStatut = factureReelleRepository.getStatistiquesParStatutByEntrepriseIdAndPeriode(
                entrepriseId, dateDebut, dateFin);
        Long totalFactures = 0L;
        Double montantTotal = 0.0;
        Long nombreFacturesPayees = 0L;
        Long nombreFacturesPartiellementPayees = 0L;
        Long nombreFacturesEnAttente = 0L;
        Double montantFacturesPayees = 0.0;
        Double montantFacturesPartiellementPayees = 0.0;
        Double montantFacturesEnAttente = 0.0;

        if (statsParStatut != null) {
            for (Object[] row : statsParStatut) {
                String statut = row[0] != null ? row[0].toString() : "";
                Long count = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                Double montant = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;

                totalFactures += count;
                montantTotal += montant;

                switch (statut) {
                    case "PAYEE":
                        nombreFacturesPayees = count;
                        montantFacturesPayees = montant;
                        break;
                    case "PARTIELLEMENT_PAYEE":
                        nombreFacturesPartiellementPayees = count;
                        montantFacturesPartiellementPayees = montant;
                        break;
                    case "EN_ATTENTE":
                        nombreFacturesEnAttente = count;
                        montantFacturesEnAttente = montant;
                        break;
                }
            }
        }

        // Total payé
        BigDecimal totalPayeBD = paiementRepository.sumMontantsByEntrepriseIdAndPeriodeFacture(
                entrepriseId, dateDebut, dateFin);
        Double montantTotalPaye = totalPayeBD != null ? totalPayeBD.doubleValue() : 0.0;
        Double montantTotalRestant = montantTotal - montantTotalPaye;

        // Top clients (Client + EntrepriseClient combinés)
        List<Object[]> clientsData = factureReelleRepository.findAllTopClientsByEntrepriseIdAndPeriode(
                entrepriseId, dateDebut, dateFin);
        List<TopClientFactureDTO> topClients = new ArrayList<>();
        if (clientsData != null) {
            for (Object[] row : clientsData) {
                TopClientFactureDTO dto = new TopClientFactureDTO();
                dto.setClientId(((Number) row[0]).longValue());
                dto.setNomClient((String) row[1]);
                dto.setType((String) row[2]);
                dto.setNombreFactures(((Number) row[3]).longValue());
                dto.setMontantTotal(row[4] != null ? ((Number) row[4]).doubleValue() : 0.0);
                topClients.add(dto);
            }
        }

        // Top créateurs
        List<Object[]> createursData = factureReelleRepository.findTopCreateursByEntrepriseIdAndPeriode(
                entrepriseId, dateDebut, dateFin);
        List<TopCreateurFactureDTO> topCreateurs = new ArrayList<>();
        if (createursData != null) {
            for (Object[] row : createursData) {
                TopCreateurFactureDTO dto = new TopCreateurFactureDTO();
                dto.setCreateurId(((Number) row[0]).longValue());
                dto.setNomCreateur((String) row[1]);
                dto.setNombreFactures(((Number) row[2]).longValue());
                dto.setMontantTotal(row[3] != null ? ((Number) row[3]).doubleValue() : 0.0);
                topCreateurs.add(dto);
            }
        }

        StatistiquesFactureReelleDTO statistiques = new StatistiquesFactureReelleDTO();
        statistiques.setTotalFactures(totalFactures);
        statistiques.setMontantTotal(Math.round(montantTotal * 100.0) / 100.0);
        statistiques.setMontantTotalPaye(Math.round(montantTotalPaye * 100.0) / 100.0);
        statistiques.setMontantTotalRestant(Math.round(montantTotalRestant * 100.0) / 100.0);
        statistiques.setNombreFacturesPayees(nombreFacturesPayees);
        statistiques.setNombreFacturesPartiellementPayees(nombreFacturesPartiellementPayees);
        statistiques.setNombreFacturesEnAttente(nombreFacturesEnAttente);
        statistiques.setMontantFacturesPayees(Math.round(montantFacturesPayees * 100.0) / 100.0);
        statistiques.setMontantFacturesPartiellementPayees(Math.round(montantFacturesPartiellementPayees * 100.0) / 100.0);
        statistiques.setMontantFacturesEnAttente(Math.round(montantFacturesEnAttente * 100.0) / 100.0);
        statistiques.setTopClients(topClients);
        statistiques.setTopCreateurs(topCreateurs);
        statistiques.setPeriode(periodeDates.periodeLabel);

        return statistiques;
    }

    private PeriodeDates calculerDatesPeriodeStatistiques(String periode) {
        if (periode == null || periode.trim().isEmpty()) {
            periode = "aujourdhui";
        }

        LocalDate dateStart;
        LocalDate dateEnd;
        String periodeLabel;

        switch (periode.toLowerCase()) {
            case "aujourdhui":
                dateStart = LocalDate.now();
                dateEnd = dateStart.plusDays(1);
                periodeLabel = "Aujourd'hui";
                break;
            case "hier":
                dateStart = LocalDate.now().minusDays(1);
                dateEnd = dateStart.plusDays(1);
                periodeLabel = "Hier";
                break;
            case "semaine":
                LocalDate aujourdhui = LocalDate.now();
                dateStart = aujourdhui.minusDays(aujourdhui.getDayOfWeek().getValue() - 1);
                dateEnd = dateStart.plusWeeks(1);
                periodeLabel = "Cette semaine";
                break;
            case "mois":
                dateStart = LocalDate.now().withDayOfMonth(1);
                dateEnd = dateStart.plusMonths(1);
                periodeLabel = "Ce mois";
                break;
            case "annee":
                dateStart = LocalDate.now().withDayOfYear(1);
                dateEnd = dateStart.plusYears(1);
                periodeLabel = "Cette année";
                break;
            default:
                dateStart = LocalDate.now();
                dateEnd = dateStart.plusDays(1);
                periodeLabel = "Aujourd'hui";
        }

        return new PeriodeDates(dateStart, dateEnd, periodeLabel);
    }

    private static class PeriodeDates {
        final LocalDate dateDebut;
        final LocalDate dateFin;
        final String periodeLabel;

        PeriodeDates(LocalDate dateDebut, LocalDate dateFin, String periodeLabel) {
            this.dateDebut = dateDebut;
            this.dateFin = dateFin;
            this.periodeLabel = periodeLabel;
        }
    }

    private String normaliserModePaiementPourStockage(String modePaiement) {
        if (modePaiement == null || modePaiement.trim().isEmpty()) {
            return null;
        }
        String normalise = modePaiement.trim().toUpperCase();
        switch (normalise) {
            case "CASH":
                return "ESPECES";
            case "MOBILE":
                return "MOBILE_MONEY";
            case "VIREMENT":
            case "TRANSFERT":
                return "VIREMENT";
            case "CHEQUE":
            case "CHECK":
                return "CHEQUE";
            case "CARTE":
            case "CARD":
                return "CARTE";
            case "RETRAIT":
                return "RETRAIT";
            case "ESPECES":
            case "MOBILE_MONEY":
            case "AUTRE":
                return normalise;
            default:
                return normalise;
        }
    }

     // Crée une entrée générale pour enregistrer un paiement de facture dans la comptabilité.
 
    private void creerEntreeGeneralePourPaiement(Paiement paiement, FactureReelle facture, User utilisateur) {
        if (paiement.getModePaiement() == null || paiement.getModePaiement().trim().isEmpty()) {
            return;
        }

        try {
            ModePaiement mode = ModePaiement.valueOf(paiement.getModePaiement().trim().toUpperCase());
            SourceDepense source = convertirModePaiementVersSource(mode);
            
            String description = "Paiement facture " + facture.getNumeroFacture();
            if (facture.getDescription() != null && !facture.getDescription().trim().isEmpty()) {
                description += " - " + facture.getDescription();
            }

            EntreeGenerale entree = new EntreeGenerale();
            entree.setNumero(genererNumeroEntreePourPaiement(utilisateur.getEntreprise().getId()));
            entree.setDesignation(description);
            entree.setCategorie(null);
            entree.setPrixUnitaire(paiement.getMontant().doubleValue());
            entree.setQuantite(1);
            entree.setMontant(paiement.getMontant().doubleValue());
            entree.setSource(source);
            entree.setModeEntree(mode);
            entree.setNumeroModeEntree(null);
            entree.setPieceJointe(null);
            entree.setEntreprise(utilisateur.getEntreprise());
            entree.setCreePar(utilisateur);
            entree.setResponsable(utilisateur);
            
            //  Lier l'entrée au paiement pour la traçabilité
            entree.setDetteId(paiement.getId());
            entree.setDetteType("PAIEMENT_FACTURE");
            entree.setDetteNumero(facture.getNumeroFacture());

            entreeGeneraleRepository.save(entree);
        } catch (IllegalArgumentException e) {
            
            System.err.println("Mode de paiement invalide pour créer l'entrée générale : " + paiement.getModePaiement());
        }
    }

     // Convertit un ModePaiement en SourceDepense pour déterminer où va l'argent
    private SourceDepense convertirModePaiementVersSource(ModePaiement mode) {
        switch (mode) {
            case ESPECES:
                return SourceDepense.CAISSE;
            case VIREMENT:
            case CHEQUE:
            case CARTE:
                return SourceDepense.BANQUE;
            case MOBILE_MONEY:
                return SourceDepense.MOBILE_MONEY;
            default:
                return SourceDepense.CAISSE;
        }
    }

     // Génère un numéro unique pour une entrée générale créée à partir d'un paiement
    private String genererNumeroEntreePourPaiement(Long entrepriseId) {
        LocalDate currentDate = LocalDate.now();
        int month = currentDate.getMonthValue();
        int year = currentDate.getYear();
        
        List<EntreeGenerale> entreesDuMois = entreeGeneraleRepository
                .findByEntrepriseIdAndMonthAndYear(entrepriseId, month, year);
        
        long newIndex = 1;
        
        if (!entreesDuMois.isEmpty()) {
            String lastNumero = entreesDuMois.get(0).getNumero();
            
            if (lastNumero != null && !lastNumero.isEmpty()) {
                Pattern pattern = Pattern.compile("EN:\\s*(\\d+)-");
                Matcher matcher = pattern.matcher(lastNumero);
                
                if (matcher.find()) {
                    try {
                        newIndex = Long.parseLong(matcher.group(1)) + 1;
                    } catch (NumberFormatException e) {
                    }
                } else {
                    Pattern fallbackPattern = Pattern.compile("(\\d+)");
                    Matcher fallbackMatcher = fallbackPattern.matcher(lastNumero);
                    if (fallbackMatcher.find()) {
                        try {
                            newIndex = Long.parseLong(fallbackMatcher.group(1)) + 1;
                        } catch (NumberFormatException e) {
                            newIndex = 1;
                        }
                    }
                }
            }
        }
        
        String indexFormate = String.format("%03d", newIndex);
        String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("MM-yyyy"));
        
        return "EN: " + indexFormate + "-" + formattedDate;
    }

}