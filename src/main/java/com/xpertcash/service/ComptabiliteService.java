package com.xpertcash.service;

import com.xpertcash.DTOs.ComptabiliteDTO;
import com.xpertcash.DTOs.ComptabiliteCompleteDTO;
import com.xpertcash.DTOs.ComptabiliteCompletePaginatedDTO;
import com.xpertcash.DTOs.CategorieDepenseDTO;
import com.xpertcash.DTOs.CategorieEntreeDTO;
import com.xpertcash.DTOs.CategorieResponseDTO;
import com.xpertcash.DTOs.DepenseGeneraleRequestDTO;
import com.xpertcash.DTOs.DepenseGeneraleResponseDTO;
import com.xpertcash.DTOs.EntreeGeneraleRequestDTO;
import com.xpertcash.DTOs.EntreeGeneraleResponseDTO;
import com.xpertcash.DTOs.PaiementDTO;
import com.xpertcash.DTOs.LigneFactureDTO;
import com.xpertcash.DTOs.TransfertFondsResponseDTO;
import com.xpertcash.DTOs.VENTE.TransactionSummaryDTO;
import com.xpertcash.DTOs.VENTE.VenteResponse;
import com.xpertcash.DTOs.VENTE.FermetureCaisseResponseDTO;
import com.xpertcash.entity.Paiement;
import com.xpertcash.entity.FactureVente;
import com.xpertcash.service.TransfertFondsService;
import com.xpertcash.service.VENTE.TransactionSummaryService;
import com.xpertcash.entity.*;
import com.xpertcash.entity.Enum.*;
import com.xpertcash.entity.VENTE.*;
import com.xpertcash.repository.*;
import com.xpertcash.repository.VENTE.*;
import com.xpertcash.DTOs.PayerDetteRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComptabiliteService {

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private VenteRepository venteRepository;

    @Autowired
    private VenteHistoriqueRepository venteHistoriqueRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private MouvementCaisseRepository mouvementCaisseRepository;

    @Autowired
    private FactureReelleRepository factureReelleRepository;
    
    @Autowired
    private FactureProformaRepository factureProformaRepository;
    
    @Autowired
    private FactureVenteRepository factureVenteRepository;

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private CaisseRepository caisseRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private DepenseGeneraleRepository depenseGeneraleRepository;

    @Autowired
    private CategorieDepenseRepository categorieDepenseRepository;

    @Autowired
    private CategorieRepository categorieRepository;

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private FournisseurRepository fournisseurRepository;

    @Autowired
    private EntreeGeneraleRepository entreeGeneraleRepository;

    @Autowired
    private TransactionSummaryService transactionSummaryService;

    @Autowired
    private TransfertFondsService transfertFondsService;

    @Autowired
    private CategorieService categorieService;

    /**
     * Récupère toutes les données comptables de l'entreprise
     */
    @Transactional(readOnly = true)
    public ComptabiliteDTO getComptabilite(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        if (user.getEntreprise() == null) {
            throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
        }

        Long entrepriseId = user.getEntreprise().getId();

        ComptabiliteDTO comptabilite = new ComptabiliteDTO();

        comptabilite.setChiffreAffaires(calculerChiffreAffaires(entrepriseId));
        comptabilite.setVentes(calculerVentes(entrepriseId));
        comptabilite.setFacturation(calculerFacturation(entrepriseId));
        comptabilite.setDepenses(calculerDepenses(entrepriseId));
        comptabilite.setDepensesGenerales(calculerDepensesGenerales(entrepriseId));
        comptabilite.setBoutiques(calculerBoutiques(entrepriseId));
        comptabilite.setBoutiquesDisponibles(listerBoutiquesDisponibles(entrepriseId));
        comptabilite.setClients(calculerClients(entrepriseId));
        comptabilite.setVendeurs(calculerVendeurs(entrepriseId));
        comptabilite.setActivites(calculerActivites(entrepriseId));

        return comptabilite;
    }

    /**
     * Payer une dette depuis la comptabilité (actuellement supporte VENTE_CREDIT).
     * Réduit la dette et met à jour le statut de la vente.
     */
    @Transactional
    public void payerDette(PayerDetteRequest request, HttpServletRequest httpRequest) {
        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);

        if (user.getEntreprise() == null) {
            throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
        }

        if (request.getId() == null) {
            throw new RuntimeException("L'id de la dette est obligatoire.");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new RuntimeException("Le type de dette est obligatoire.");
        }
        if (request.getMontant() == null || request.getMontant() <= 0) {
            throw new RuntimeException("Le montant du paiement doit être supérieur à 0.");
        }
        if (request.getModePaiement() == null || request.getModePaiement().isBlank()) {
            throw new RuntimeException("Le mode de paiement est obligatoire.");
        }

        String type = request.getType();

        // Gestion des différents types de dettes payables depuis la comptabilité
        if ("VENTE_CREDIT".equals(type)) {
            payerVenteCreditDepuisComptabilite(request, user);
        } else if ("ENTREE_DETTE".equals(type)) {
            payerEntreeDetteDepuisComptabilite(request, user);
        } else {
            throw new RuntimeException("Type de dette non supporté pour le paiement depuis la comptabilité : " + type);
        }
    }

    private void payerVenteCreditDepuisComptabilite(PayerDetteRequest request, User user) {
        Vente vente = venteRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Vente introuvable"));

        if (vente.getBoutique() == null || vente.getBoutique().getEntreprise() == null) {
            throw new RuntimeException("La vente n'est pas rattachée à une entreprise.");
        }

        if (!vente.getBoutique().getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette vente n'appartient pas à votre entreprise.");
        }

        if (vente.getModePaiement() != ModePaiement.CREDIT) {
            throw new RuntimeException("Cette vente n'est pas une vente à crédit.");
        }

        double total = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;
        double dejaRembourse = vente.getMontantTotalRembourse() != null ? vente.getMontantTotalRembourse() : 0.0;
        double restant = total - dejaRembourse;

        if (restant <= 0) {
            throw new RuntimeException("Cette vente à crédit est déjà totalement payée.");
        }
        if (request.getMontant() > restant) {
            throw new RuntimeException("Le montant du paiement (" + request.getMontant() +
                    ") dépasse le montant restant dû (" + restant + ").");
        }

        double montantPaiement = request.getMontant();

        // 💰 Enregistrer l'entrée de trésorerie dans le bon canal (CAISSE / BANQUE / MOBILE_MONEY)
        ModePaiement mode;
        try {
            mode = ModePaiement.valueOf(request.getModePaiement());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Mode de paiement invalide : " + request.getModePaiement());
        }

        SourceDepense source;
        switch (mode) {
            case ESPECES:
                source = SourceDepense.CAISSE;
                break;
            case VIREMENT:
            case CHEQUE:
            case CARTE:
                source = SourceDepense.BANQUE;
                break;
            case MOBILE_MONEY:
                source = SourceDepense.MOBILE_MONEY;
                break;
            default:
                source = SourceDepense.CAISSE;
                break;
        }

        EntreeGenerale entree = new EntreeGenerale();
        entree.setDesignation("Paiement crédit POS vente ID " + vente.getId());
        entree.setCategorie(null);
        entree.setPrixUnitaire(montantPaiement);
        entree.setQuantite(1);
        entree.setSource(source);
        entree.setModeEntree(mode);
        entree.setNumeroModeEntree(null);
        entree.setPieceJointe(null);
        entree.setEntreprise(user.getEntreprise());
        entree.setCreePar(user);
        entree.setResponsable(user);
        entreeGeneraleRepository.save(entree);

        // Mise à jour de la vente (dette)
        double nouveauTotalRembourse = dejaRembourse + montantPaiement;
        vente.setMontantTotalRembourse(nouveauTotalRembourse);
        vente.setDateDernierRemboursement(LocalDateTime.now());

        if (nouveauTotalRembourse >= total) {
            vente.setStatus(VenteStatus.PAYEE);
        } else {
            vente.setStatus(VenteStatus.EN_COURS);
        }

        // On peut refléter le montant payé cumulé dans montantPaye
        vente.setMontantPaye(nouveauTotalRembourse);

        venteRepository.save(vente);

        // Historique
        VenteHistorique historique = new VenteHistorique();
        historique.setVente(vente);
        historique.setDateAction(LocalDateTime.now());
        historique.setAction("PAIEMENT_VENTE_CREDIT");
        historique.setDetails("Paiement de " + montantPaiement + " sur vente à crédit depuis la comptabilité par "
                + user.getNomComplet() + " via " + request.getModePaiement());
        historique.setMontant(montantPaiement);
        venteHistoriqueRepository.save(historique);
    }

    /**
     * Encaisse une dette créée via EntreeGenerale avec source = DETTE.
     * - Réduit le montant restant de l'entrée DETTE
     * - Crée une nouvelle EntreeGenerale réelle (CAISSE/BANQUE/MOBILE_MONEY) pour l'encaissement
     */
    private void payerEntreeDetteDepuisComptabilite(PayerDetteRequest request, User user) {
        EntreeGenerale entreeDette = entreeGeneraleRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Entrée de dette introuvable"));

        if (entreeDette.getEntreprise() == null || !entreeDette.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Accès interdit : cette dette n'appartient pas à votre entreprise.");
        }

        if (entreeDette.getSource() != SourceDepense.DETTE) {
            throw new RuntimeException("Cette entrée n'est pas une dette (source différente de DETTE).");
        }

        double montantInitial = entreeDette.getMontant() != null ? entreeDette.getMontant() : 0.0;
        if (montantInitial <= 0) {
            throw new RuntimeException("Cette dette est déjà totalement encaissée.");
        }

        double montantPaiement = request.getMontant();
        if (montantPaiement > montantInitial) {
            throw new RuntimeException("Le montant du paiement (" + montantPaiement +
                    ") dépasse le montant restant dû (" + montantInitial + ").");
        }

        // Déterminer la source réelle (CAISSE / BANQUE / MOBILE_MONEY) en fonction du mode de paiement
        ModePaiement mode;
        try {
            mode = ModePaiement.valueOf(request.getModePaiement().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Mode de paiement invalide : " + request.getModePaiement());
        }

        SourceDepense sourceReelle;
        switch (mode) {
            case ESPECES:
                sourceReelle = SourceDepense.CAISSE;
                break;
            case VIREMENT:
            case CHEQUE:
            case CARTE:
                sourceReelle = SourceDepense.BANQUE;
                break;
            case MOBILE_MONEY:
                sourceReelle = SourceDepense.MOBILE_MONEY;
                break;
            default:
                sourceReelle = SourceDepense.CAISSE;
                break;
        }

        // 1️⃣ Créer l'entrée réelle encaissée
        EntreeGenerale encaissement = new EntreeGenerale();
        encaissement.setNumero(genererNumeroEntree(user.getEntreprise().getId()));
        encaissement.setDesignation(
                (entreeDette.getDesignation() != null ? entreeDette.getDesignation() : "Encaissement dette")
                        + " (paiement)");
        encaissement.setCategorie(entreeDette.getCategorie());
        encaissement.setPrixUnitaire(montantPaiement);
        encaissement.setQuantite(1);
        encaissement.setMontant(montantPaiement);
        encaissement.setSource(sourceReelle);
        encaissement.setModeEntree(mode);
        encaissement.setNumeroModeEntree(null);
        encaissement.setPieceJointe(null);
        encaissement.setEntreprise(user.getEntreprise());
        encaissement.setCreePar(user);
        encaissement.setResponsable(entreeDette.getResponsable() != null ? entreeDette.getResponsable() : user);
        entreeGeneraleRepository.save(encaissement);

        // 2️⃣ Réduire ou clôturer la dette initiale
        double restant = montantInitial - montantPaiement;
        entreeDette.setMontant(restant);
        if (restant <= 0) {
            // On peut marquer la dette comme encaissée en changeant la source
            entreeDette.setSource(sourceReelle);
        }
        entreeGeneraleRepository.save(entreeDette);
    }

    /**
     * Calcule le chiffre d'affaires (revenus totaux)
     */
    private ComptabiliteDTO.ChiffreAffairesDTO calculerChiffreAffaires(Long entrepriseId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // Pour le mois
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);

        // Pour l'année
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        LocalDate lastDayOfYear = today.withDayOfYear(today.lengthOfYear());
        LocalDateTime startOfYear = firstDayOfYear.atStartOfDay();
        LocalDateTime endOfYear = lastDayOfYear.atTime(LocalTime.MAX);

        // Récupérer factures réelles et proforma
        List<FactureReelle> toutesFacturesReelles = factureReelleRepository.findByEntrepriseId(entrepriseId);
        List<FactureProForma> toutesFacturesProforma = factureProformaRepository.findByEntrepriseId(entrepriseId);
        
        // Calculer le total des paiements pour toutes les factures (optimisation N+1)
        double totalPaiementsFactures = 0.0;
        if (!toutesFacturesReelles.isEmpty()) {
            List<Long> factureIds = toutesFacturesReelles.stream().map(FactureReelle::getId).collect(Collectors.toList());
            List<Object[]> paiements = paiementRepository.sumMontantsByFactureReelleIds(factureIds);
            for (Object[] paiement : paiements) {
                totalPaiementsFactures += ((Number) paiement[1]).doubleValue();
            }
        }

        // Calculer les ventes nettes (avec remboursements)
        double totalVentes = calculerVentesNet(entrepriseId, null, null);
        double ventesJour = calculerVentesNet(entrepriseId, startOfDay, endOfDay);
        double ventesMois = calculerVentesNet(entrepriseId, startOfMonth, endOfMonth);
        double ventesAnnee = calculerVentesNet(entrepriseId, startOfYear, endOfYear);

        // Calculer les montants de factures
        double totalFacturesEmises = toutesFacturesReelles.stream()
                .mapToDouble(FactureReelle::getTotalFacture)
                .sum();
        double totalFacturesProforma = toutesFacturesProforma.stream()
                .mapToDouble(FactureProForma::getTotalFacture)
                .sum();
        int nombreFacturesReelles = toutesFacturesReelles.size();
        int nombreFacturesProforma = toutesFacturesProforma.size();

        // Construire les détails des ventes
        List<Vente> ventes = venteRepository.findAllByEntrepriseId(entrepriseId);
        List<Long> venteIds = ventes.stream().map(Vente::getId).collect(Collectors.toList());
        Map<Long, Double> remboursementsMap = venteIds.isEmpty() ? java.util.Collections.emptyMap() :
                venteHistoriqueRepository.sumRemboursementsByVenteIds(venteIds)
                        .stream()
                        .collect(Collectors.toMap(
                                obj -> (Long) obj[0],
                                obj -> ((Number) obj[1]).doubleValue()
                        ));

        // Récupérer les numéros de tickets via FactureVente
        Map<Long, String> venteIdToNumero = new HashMap<>();
        List<FactureVente> facturesVente = factureVenteRepository.findAllByEntrepriseId(entrepriseId);
        for (FactureVente fv : facturesVente) {
            if (fv.getVente() != null) {
                venteIdToNumero.put(fv.getVente().getId(), fv.getNumeroFacture());
            }
        }

        List<ComptabiliteDTO.VenteCADetail> ventesDetails = ventes.stream().map(v -> {
            double montant = v.getMontantTotal() != null ? v.getMontantTotal() : 0.0;
            double remb = remboursementsMap.getOrDefault(v.getId(), 0.0);
            Double net = montant - remb;
            String numeroTicket = venteIdToNumero.getOrDefault(v.getId(), "VENTE-" + v.getId());
            String mode = v.getModePaiement() != null ? v.getModePaiement().name() : null;
            String statut = v.getStatus() != null ? v.getStatus().name() : null;
            return new ComptabiliteDTO.VenteCADetail(
                    v.getId(),
                    numeroTicket,
                    v.getDateVente(),
                    mode,
                    v.getRemiseGlobale(),
                    net,
                    statut
            );
        }).collect(Collectors.toList());

        // Détails factures (lisibles: remise/TVA et reste à payer)
        List<Long> factureIds = toutesFacturesReelles.stream().map(FactureReelle::getId).collect(Collectors.toList());
        Map<Long, Double> paiementsParFacture = new HashMap<>();
        Map<Long, String> dernierEncaisseurParFacture = new HashMap<>();
        if (!factureIds.isEmpty()) {
            List<Object[]> paiementsAgg = paiementRepository.sumMontantsByFactureReelleIds(factureIds);
            for (Object[] row : paiementsAgg) {
                Long fid = (Long) row[0];
                double somme = ((Number) row[1]).doubleValue();
                paiementsParFacture.put(fid, somme);
            }
            // Récupérer tous les paiements pour déterminer le dernier encaisseur
            List<Paiement> paiementsList = paiementRepository.findByFactureReelle_IdIn(factureIds);
            java.util.Map<Long, Paiement> dernierPaiement = paiementsList.stream()
                    .filter(p -> p.getDatePaiement() != null)
                    .collect(java.util.stream.Collectors.toMap(
                            p -> p.getFactureReelle() != null ? p.getFactureReelle().getId() : -1L,
                            p -> p,
                            (p1, p2) -> p1.getDatePaiement().isAfter(p2.getDatePaiement()) ? p1 : p2
                    ));
            for (java.util.Map.Entry<Long, Paiement> e : dernierPaiement.entrySet()) {
                Paiement p = e.getValue();
                String nom = p.getEncaissePar() != null ? p.getEncaissePar().getNomComplet() : null;
                dernierEncaisseurParFacture.put(e.getKey(), nom);
            }
        }
        List<ComptabiliteDTO.FactureDetail> factureDetails = toutesFacturesReelles.stream().map(f -> {
            double paye = paiementsParFacture.getOrDefault(f.getId(), 0.0);
            double restant = (f.getTotalFacture() != 0 ? f.getTotalFacture() : 0.0) - paye;
            String statutPaiement = f.getStatutPaiement() != null ? f.getStatutPaiement().name() : null;
            String encaissePar = dernierEncaisseurParFacture.getOrDefault(f.getId(), null);
            ComptabiliteDTO.FactureDetail d = new ComptabiliteDTO.FactureDetail();
            d.setFactureId(f.getId());
            d.setNumeroFacture(f.getNumeroFacture());
            d.setDateCreation(f.getDateCreation());
            d.setTotalHT(f.getTotalHT());
            d.setRemise(f.getRemise());
            d.setTva(f.isTva());
            d.setTotalFacture(f.getTotalFacture());
            d.setMontantPaye(paye);
            d.setMontantRestant(restant);
            d.setStatutPaiement(statutPaiement);
            d.setEncaissePar(encaissePar);
            d.setType("REELLE");
            try { d.setStatut(f.getStatut() != null ? f.getStatut().name() : null); } catch (Exception ignore) { d.setStatut(null); }
            return d;
        }).collect(Collectors.toList());

        // Calculer les dépenses générales
        List<DepenseGenerale> toutesDepensesGenerales = depenseGeneraleRepository.findByEntrepriseId(entrepriseId);
        
        // Filtrer les dépenses générales par période
        List<DepenseGenerale> depensesGeneralesJour = toutesDepensesGenerales.stream()
                .filter(d -> d.getDateCreation() != null && 
                        !d.getDateCreation().isBefore(startOfDay) && 
                        !d.getDateCreation().isAfter(endOfDay))
                .collect(Collectors.toList());

        List<DepenseGenerale> depensesGeneralesMois = toutesDepensesGenerales.stream()
                .filter(d -> d.getDateCreation() != null && 
                        !d.getDateCreation().isBefore(startOfMonth) && 
                        !d.getDateCreation().isAfter(endOfMonth))
                .collect(Collectors.toList());

        List<DepenseGenerale> depensesGeneralesAnnee = toutesDepensesGenerales.stream()
                .filter(d -> d.getDateCreation() != null && 
                        !d.getDateCreation().isBefore(startOfYear) && 
                        !d.getDateCreation().isAfter(endOfYear))
                .collect(Collectors.toList());

        // Calculer les montants des dépenses générales
        double totalDepensesGenerales = toutesDepensesGenerales.stream()
                .mapToDouble(d -> d.getMontant() != null ? d.getMontant() : 0.0)
                .sum();
        double depensesGeneralesDuJour = depensesGeneralesJour.stream()
                .mapToDouble(d -> d.getMontant() != null ? d.getMontant() : 0.0)
                .sum();
        double depensesGeneralesDuMois = depensesGeneralesMois.stream()
                .mapToDouble(d -> d.getMontant() != null ? d.getMontant() : 0.0)
                .sum();
        double depensesGeneralesDeLAnnee = depensesGeneralesAnnee.stream()
                .mapToDouble(d -> d.getMontant() != null ? d.getMontant() : 0.0)
                .sum();

        // Total chiffre d'affaires = ventes + paiements de factures
        double total = totalVentes + totalPaiementsFactures;

        ComptabiliteDTO.ChiffreAffairesDTO dto = new ComptabiliteDTO.ChiffreAffairesDTO();
        dto.setTotal(total);
        dto.setDuJour(ventesJour);
        dto.setDuMois(ventesMois);
        dto.setDeLAnnee(ventesAnnee);
        dto.setTotalVentes(totalVentes);
        dto.setTotalFactures(totalFacturesEmises);
        dto.setTotalPaiementsFactures(totalPaiementsFactures);
        dto.setTotalDepensesGenerales(totalDepensesGenerales);
        dto.setDepensesGeneralesDuJour(depensesGeneralesDuJour);
        dto.setDepensesGeneralesDuMois(depensesGeneralesDuMois);
        dto.setDepensesGeneralesDeLAnnee(depensesGeneralesDeLAnnee);
        dto.setVentesDetails(ventesDetails);
        dto.setFactureDetails(factureDetails);
        dto.setNombreFacturesReelles(nombreFacturesReelles);
        dto.setMontantFacturesReelles(totalFacturesEmises);
        dto.setNombreFacturesProforma(nombreFacturesProforma);
        dto.setMontantFacturesProforma(totalFacturesProforma);
        return dto;
    }

    /**
     * Calcule les ventes nettes (en déduisant les remboursements)
     */
    private double calculerVentesNet(Long entrepriseId, LocalDateTime debut, LocalDateTime fin) {
        List<Vente> ventes;
        
        if (debut != null && fin != null) {
            ventes = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(entrepriseId, debut, fin);
        } else {
            ventes = venteRepository.findAllByEntrepriseId(entrepriseId);
        }

        List<Long> venteIds = ventes.stream().map(Vente::getId).collect(Collectors.toList());
        
        if (venteIds.isEmpty()) {
            return 0.0;
        }

        Map<Long, Double> remboursementsMap = venteHistoriqueRepository.sumRemboursementsByVenteIds(venteIds)
                .stream()
                .collect(Collectors.toMap(
                        obj -> (Long) obj[0],
                        obj -> ((Number) obj[1]).doubleValue()
                ));

        double total = 0.0;
        for (Vente vente : ventes) {
            double montantVente = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;
            double remboursements = remboursementsMap.getOrDefault(vente.getId(), 0.0);
            total += montantVente - remboursements;
        }

        return total;
    }

    /**
     * Calcule les statistiques des ventes
     */
    private ComptabiliteDTO.VentesDTO calculerVentes(Long entrepriseId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        LocalDate lastDayOfYear = today.withDayOfYear(today.lengthOfYear());
        LocalDateTime startOfYear = firstDayOfYear.atStartOfDay();
        LocalDateTime endOfYear = lastDayOfYear.atTime(LocalTime.MAX);

        List<Vente> toutesVentes = venteRepository.findAllByEntrepriseId(entrepriseId);
        List<Vente> ventesJour = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
                entrepriseId, startOfDay, endOfDay);
        List<Vente> ventesMois = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
                entrepriseId, startOfMonth, endOfMonth);
        List<Vente> ventesAnnee = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
                entrepriseId, startOfYear, endOfYear);

        double montantTotal = calculerVentesNet(entrepriseId, null, null);
        double montantDuJour = calculerVentesNet(entrepriseId, startOfDay, endOfDay);
        double montantDuMois = calculerVentesNet(entrepriseId, startOfMonth, endOfMonth);
        double montantDeLAnnee = calculerVentesNet(entrepriseId, startOfYear, endOfYear);

        // Nombre de ventes annulées (considérées comme totalement remboursées)
        int annulees = (int) toutesVentes.stream()
                .filter(v -> v.getStatus() != null && v.getStatus() == VenteStatus.REMBOURSEE)
                .count();

        return new ComptabiliteDTO.VentesDTO(
                toutesVentes.size(),
                montantTotal,
                ventesJour.size(),
                montantDuJour,
                ventesMois.size(),
                montantDuMois,
                ventesAnnee.size(),
                montantDeLAnnee,
                annulees
        );
    }

    /**
     * Calcule les statistiques de facturation
     */
    private ComptabiliteDTO.FacturationDTO calculerFacturation(Long entrepriseId) {
        LocalDate today = LocalDate.now();

        List<FactureReelle> toutesFactures = factureReelleRepository.findByEntrepriseId(entrepriseId);
        List<FactureProForma> toutesProforma = factureProformaRepository.findByEntrepriseId(entrepriseId);
        
        // Filtrer par date pour les stats périodiques
        List<FactureReelle> facturesJour = toutesFactures.stream()
                .filter(f -> f.getDateCreation() != null && f.getDateCreation().equals(today))
                .collect(Collectors.toList());
        
        List<FactureReelle> facturesMois = toutesFactures.stream()
                .filter(f -> f.getDateCreation() != null && 
                        f.getDateCreation().getMonth() == today.getMonth() && 
                        f.getDateCreation().getYear() == today.getYear())
                .collect(Collectors.toList());
        
        List<FactureReelle> facturesAnnee = toutesFactures.stream()
                .filter(f -> f.getDateCreation() != null && f.getDateCreation().getYear() == today.getYear())
                .collect(Collectors.toList());

        double montantTotalFactures = toutesFactures.stream()
                .mapToDouble(FactureReelle::getTotalFacture)
                .sum();

        // Calculer le montant payé et impayé
        double montantPaye = 0.0;
        List<Long> factureIds = toutesFactures.stream().map(FactureReelle::getId).collect(Collectors.toList());
        if (!factureIds.isEmpty()) {
            List<Object[]> paiements = paiementRepository.sumMontantsByFactureReelleIds(factureIds);
            for (Object[] paiement : paiements) {
                montantPaye += ((Number) paiement[1]).doubleValue();
            }
        }

        double montantImpaye = montantTotalFactures - montantPaye;

        double montantDuJour = facturesJour.stream()
                .mapToDouble(FactureReelle::getTotalFacture)
                .sum();

        double montantDuMois = facturesMois.stream()
                .mapToDouble(FactureReelle::getTotalFacture)
                .sum();

        double montantDeLAnnee = facturesAnnee.stream()
                .mapToDouble(FactureReelle::getTotalFacture)
                .sum();

        // Détails factures pour lisibilité (remise/tva et reste à payer)
        Map<Long, Double> paiementsParFacture = new HashMap<>();
        Map<Long, String> dernierEncaisseurParFacture = new HashMap<>();
        if (!factureIds.isEmpty()) {
            List<Object[]> paiementsAgg = paiementRepository.sumMontantsByFactureReelleIds(factureIds);
            for (Object[] row : paiementsAgg) {
                Long fid = (Long) row[0];
                double somme = ((Number) row[1]).doubleValue();
                paiementsParFacture.put(fid, somme);
            }

            // Déterminer le dernier encaisseur par facture à partir des paiements
            List<Paiement> paiementsList = paiementRepository.findByFactureReelle_IdIn(factureIds);
            java.util.Map<Long, Paiement> dernierPaiement = paiementsList.stream()
                    .filter(p -> p.getDatePaiement() != null)
                    .collect(java.util.stream.Collectors.toMap(
                            p -> p.getFactureReelle() != null ? p.getFactureReelle().getId() : -1L,
                            p -> p,
                            (p1, p2) -> p1.getDatePaiement().isAfter(p2.getDatePaiement()) ? p1 : p2
                    ));
            for (java.util.Map.Entry<Long, Paiement> e : dernierPaiement.entrySet()) {
                Paiement p = e.getValue();
                String nom = p.getEncaissePar() != null ? p.getEncaissePar().getNomComplet() : null;
                dernierEncaisseurParFacture.put(e.getKey(), nom);
            }
        }

        List<ComptabiliteDTO.FactureDetail> details = toutesFactures.stream().map(f -> {
            double paye = paiementsParFacture.getOrDefault(f.getId(), 0.0);
            double restant = (f.getTotalFacture() != 0 ? f.getTotalFacture() : 0.0) - paye;
            String statut = f.getStatutPaiement() != null ? f.getStatutPaiement().name() : null;
            ComptabiliteDTO.FactureDetail d = new ComptabiliteDTO.FactureDetail();
            d.setFactureId(f.getId());
            d.setNumeroFacture(f.getNumeroFacture());
            d.setDateCreation(f.getDateCreation());
            d.setTotalHT(f.getTotalHT());
            d.setRemise(f.getRemise());
            d.setTva(f.isTva());
            d.setTotalFacture(f.getTotalFacture());
            d.setMontantPaye(paye);
            d.setMontantRestant(restant);
            d.setStatutPaiement(statut);
            d.setEncaissePar(dernierEncaisseurParFacture.getOrDefault(f.getId(), null));
            d.setType("REELLE");
            try { d.setStatut(f.getStatut() != null ? f.getStatut().name() : null); } catch (Exception e) { d.setStatut(null); }
            return d;
        }).collect(java.util.stream.Collectors.toList());

        // Ajouter les PROFORMA aux détails
        List<ComptabiliteDTO.FactureDetail> proformaDetails = toutesProforma.stream().map(pf -> {
            ComptabiliteDTO.FactureDetail d = new ComptabiliteDTO.FactureDetail();
            d.setFactureId(pf.getId());
            d.setNumeroFacture(pf.getNumeroFacture());
            d.setDateCreation(pf.getDateCreation() != null ? pf.getDateCreation().toLocalDate() : null);
            try { d.setTotalHT(pf.getTotalHT()); } catch (Exception ignore) {}
            try { d.setRemise(pf.getRemise()); } catch (Exception ignore) {}
            try { d.setTva(pf.isTva()); } catch (Exception ignore) {}
            d.setTotalFacture(pf.getTotalFacture());
            d.setMontantPaye(0.0);
            d.setMontantRestant(pf.getTotalFacture());
            d.setStatutPaiement(null);
            d.setEncaissePar(null);
            d.setType("PROFORMA");
            try { d.setStatut(pf.getStatut() != null ? pf.getStatut().name() : null); } catch (Exception e) { d.setStatut(null); }
            return d;
        }).collect(java.util.stream.Collectors.toList());

        details.addAll(proformaDetails);

        ComptabiliteDTO.FacturationDTO dto = new ComptabiliteDTO.FacturationDTO();
        dto.setNombreTotalFactures(toutesFactures.size());
        dto.setMontantTotalFactures(montantTotalFactures);
        dto.setMontantPaye(montantPaye);
        dto.setMontantImpaye(montantImpaye);
        dto.setDuJour(facturesJour.size());
        dto.setMontantDuJour(montantDuJour);
        dto.setDuMois(facturesMois.size());
        dto.setMontantDuMois(montantDuMois);
        dto.setDeLAnnee(facturesAnnee.size());
        dto.setMontantDeLAnnee(montantDeLAnnee);
        dto.setDetails(details);
        return dto;
    }

    /**
     *  statistiques des dépenses
     */
    private ComptabiliteDTO.DepensesDTO calculerDepenses(Long entrepriseId) {
        List<Boutique> boutiques = boutiqueRepository.findByEntrepriseId(entrepriseId);
        List<Long> boutiqueIds = boutiques.stream().map(Boutique::getId).collect(Collectors.toList());

        if (boutiqueIds.isEmpty()) {
            ComptabiliteDTO.DepensesDTO dto = new ComptabiliteDTO.DepensesDTO();
            dto.setNombreTotal(0);
            dto.setMontantTotal(0.0);
            dto.setDuJour(0);
            dto.setMontantDuJour(0.0);
            dto.setDuMois(0);
            dto.setMontantDuMois(0.0);
            dto.setDeLAnnee(0);
            dto.setMontantDeLAnnee(0.0);
            dto.setDetails(java.util.Collections.emptyList());
            return dto;
        }

        // Récupérer les mouvements de type DEPENSE
        List<MouvementCaisse> toutesDepenses = mouvementCaisseRepository
                .findByCaisse_Boutique_Entreprise_IdAndTypeMouvement(
                        entrepriseId, TypeMouvementCaisse.DEPENSE);

        // Dépenses d'aujourd'hui
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        List<MouvementCaisse> depensesJour = mouvementCaisseRepository
                .findByCaisse_Boutique_Entreprise_IdAndTypeMouvementAndDateMouvementBetween(
                        entrepriseId, TypeMouvementCaisse.DEPENSE, startOfDay, endOfDay);

        // Dépenses du mois
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        List<MouvementCaisse> depensesMois = mouvementCaisseRepository
                .findByCaisse_Boutique_Entreprise_IdAndTypeMouvementAndDateMouvementBetween(
                        entrepriseId, TypeMouvementCaisse.DEPENSE, startOfMonth, endOfMonth);

        // Dépenses de l'année
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        LocalDate lastDayOfYear = today.withDayOfYear(today.lengthOfYear());
        LocalDateTime startOfYear = firstDayOfYear.atStartOfDay();
        LocalDateTime endOfYear = lastDayOfYear.atTime(LocalTime.MAX);
        List<MouvementCaisse> depensesAnnee = mouvementCaisseRepository
                .findByCaisse_Boutique_Entreprise_IdAndTypeMouvementAndDateMouvementBetween(
                        entrepriseId, TypeMouvementCaisse.DEPENSE, startOfYear, endOfYear);

        double montantTotal = toutesDepenses.stream()
                .mapToDouble(m -> m.getMontant() != null ? m.getMontant() : 0.0)
                .sum();
        double montantDuJour = depensesJour.stream()
                .mapToDouble(m -> m.getMontant() != null ? m.getMontant() : 0.0)
                .sum();
        double montantDuMois = depensesMois.stream()
                .mapToDouble(m -> m.getMontant() != null ? m.getMontant() : 0.0)
                .sum();
        double montantDeLAnnee = depensesAnnee.stream()
                .mapToDouble(m -> m.getMontant() != null ? m.getMontant() : 0.0)
                .sum();

        // Détails des dépenses (Date, Libellé, Méthode, Montant)
        List<ComptabiliteDTO.DepenseDetail> details = toutesDepenses.stream().map(d -> {
            ComptabiliteDTO.DepenseDetail dd = new ComptabiliteDTO.DepenseDetail();
            dd.setDate(d.getDateMouvement());
            dd.setLibelle(d.getDescription());
            dd.setMethode(libelleModePaiement(d.getModePaiement()));
            dd.setMontant(d.getMontant());
            return dd;
        }).collect(Collectors.toList());

        ComptabiliteDTO.DepensesDTO dto = new ComptabiliteDTO.DepensesDTO();
        dto.setNombreTotal(toutesDepenses.size());
        dto.setMontantTotal(montantTotal);
        dto.setDuJour(depensesJour.size());
        dto.setMontantDuJour(montantDuJour);
        dto.setDuMois(depensesMois.size());
        dto.setMontantDuMois(montantDuMois);
        dto.setDeLAnnee(depensesAnnee.size());
        dto.setMontantDeLAnnee(montantDeLAnnee);
        dto.setDetails(details);
        return dto;
    }

    /**
     * Calcule les statistiques des dépenses générales
     */
    private ComptabiliteDTO.DepensesGeneralesDTO calculerDepensesGenerales(Long entrepriseId) {
        // Récupérer toutes les dépenses générales de l'entreprise
        List<DepenseGenerale> toutesDepensesGenerales = depenseGeneraleRepository.findByEntrepriseId(entrepriseId);

        // Définir les périodes
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        LocalDate lastDayOfYear = today.withDayOfYear(today.lengthOfYear());
        LocalDateTime startOfYear = firstDayOfYear.atStartOfDay();
        LocalDateTime endOfYear = lastDayOfYear.atTime(LocalTime.MAX);

        // Filtrer par période
        List<DepenseGenerale> depensesJour = toutesDepensesGenerales.stream()
                .filter(d -> d.getDateCreation() != null && 
                        !d.getDateCreation().isBefore(startOfDay) && 
                        !d.getDateCreation().isAfter(endOfDay))
                .collect(Collectors.toList());

        List<DepenseGenerale> depensesMois = toutesDepensesGenerales.stream()
                .filter(d -> d.getDateCreation() != null && 
                        !d.getDateCreation().isBefore(startOfMonth) && 
                        !d.getDateCreation().isAfter(endOfMonth))
                .collect(Collectors.toList());

        List<DepenseGenerale> depensesAnnee = toutesDepensesGenerales.stream()
                .filter(d -> d.getDateCreation() != null && 
                        !d.getDateCreation().isBefore(startOfYear) && 
                        !d.getDateCreation().isAfter(endOfYear))
                .collect(Collectors.toList());

        // Calculer les montants
        double montantTotal = toutesDepensesGenerales.stream()
                .mapToDouble(d -> d.getMontant() != null ? d.getMontant() : 0.0)
                .sum();
        double montantDuJour = depensesJour.stream()
                .mapToDouble(d -> d.getMontant() != null ? d.getMontant() : 0.0)
                .sum();
        double montantDuMois = depensesMois.stream()
                .mapToDouble(d -> d.getMontant() != null ? d.getMontant() : 0.0)
                .sum();
        double montantDeLAnnee = depensesAnnee.stream()
                .mapToDouble(d -> d.getMontant() != null ? d.getMontant() : 0.0)
                .sum();

        // Créer les détails
        List<ComptabiliteDTO.DepenseGeneraleDetail> details = toutesDepensesGenerales.stream()
                .map(this::mapToDepenseGeneraleDetail)
                .collect(Collectors.toList());

        ComptabiliteDTO.DepensesGeneralesDTO dto = new ComptabiliteDTO.DepensesGeneralesDTO();
        dto.setNombreTotal(toutesDepensesGenerales.size());
        dto.setMontantTotal(montantTotal);
        dto.setDuJour(depensesJour.size());
        dto.setMontantDuJour(montantDuJour);
        dto.setDuMois(depensesMois.size());
        dto.setMontantDuMois(montantDuMois);
        dto.setDeLAnnee(depensesAnnee.size());
        dto.setMontantDeLAnnee(montantDeLAnnee);
        dto.setDetails(details);
        
        return dto;
    }

    /**
     * Mappe une DepenseGenerale vers DepenseGeneraleDetail
     */
    private ComptabiliteDTO.DepenseGeneraleDetail mapToDepenseGeneraleDetail(DepenseGenerale depense) {
        ComptabiliteDTO.DepenseGeneraleDetail detail = new ComptabiliteDTO.DepenseGeneraleDetail();
        detail.setId(depense.getId());
        detail.setNumero(depense.getNumero());
        detail.setDesignation(depense.getDesignation());
        detail.setCategorieNom(depense.getCategorie() != null ? depense.getCategorie().getNom() : null);
        detail.setPrixUnitaire(depense.getPrixUnitaire());
        detail.setQuantite(depense.getQuantite());
        detail.setMontant(depense.getMontant());
        detail.setSource(depense.getSource() != null ? depense.getSource().name() : null);
        detail.setOrdonnateur(depense.getOrdonnateur() != null ? depense.getOrdonnateur().name() : null);
        detail.setNumeroCheque(depense.getNumeroCheque());
        detail.setTypeCharge(depense.getTypeCharge() != null ? depense.getTypeCharge().name() : null);
        detail.setProduitNom(depense.getProduit() != null ? depense.getProduit().getNom() : null);
        detail.setFournisseurNom(depense.getFournisseur() != null ? 
                (depense.getFournisseur().getNomComplet() != null ? 
                        depense.getFournisseur().getNomComplet() : 
                        depense.getFournisseur().getNomSociete()) : null);
        detail.setPieceJointe(depense.getPieceJointe());
        detail.setDateCreation(depense.getDateCreation());
        detail.setCreeParNom(depense.getCreePar() != null ? depense.getCreePar().getNomComplet() : null);
        return detail;
    }

    private String libelleModePaiement(ModePaiement mode) {
        if (mode == null) return "Espèces";
        switch (mode.name()) {
            case "ESPECES":
                return "Espèces";
            case "CHEQUE":
                return "Chèque";
            case "CARTE":
                return "Carte";
            case "MOBILE_MONEY":
                return "Mobile money";
            case "VIREMENT":
                return "Virement";
            default:
                return mode.name();
        }
    }

    /**
     * Calcule les statistiques par boutique
     */
    private List<ComptabiliteDTO.BoutiqueInfoDTO> calculerBoutiques(Long entrepriseId) {
        List<Boutique> boutiques = boutiqueRepository.findByEntrepriseId(entrepriseId);
        List<ComptabiliteDTO.BoutiqueInfoDTO> boutiquesInfo = new ArrayList<>();

        for (Boutique boutique : boutiques) {
            List<Vente> ventes = venteRepository.findByBoutiqueId(boutique.getId());
            
            // Calculer les ventes nettes de cette boutique
            List<Long> venteIds = ventes.stream().map(Vente::getId).collect(Collectors.toList());
            Map<Long, Double> remboursementsMap = new HashMap<>();
            if (!venteIds.isEmpty()) {
                remboursementsMap = venteHistoriqueRepository.sumRemboursementsByVenteIds(venteIds)
                        .stream()
                        .collect(Collectors.toMap(
                                obj -> (Long) obj[0],
                                obj -> ((Number) obj[1]).doubleValue()
                        ));
            }

            double chiffreAffaires = 0.0;
            for (Vente vente : ventes) {
                double montantVente = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;
                double remboursements = remboursementsMap.getOrDefault(vente.getId(), 0.0);
                chiffreAffaires += montantVente - remboursements;
            }

            // Calculer les dépenses de cette boutique
            List<Caisse> caisses = getCaissesForBoutique(boutique.getId());
            List<Long> caisseIds = caisses.stream().map(Caisse::getId).collect(Collectors.toList());

            List<MouvementCaisse> depenses = new ArrayList<>();
            if (!caisseIds.isEmpty()) {
                depenses = mouvementCaisseRepository
                        .findByCaisseIdInAndTypeMouvement(caisseIds, TypeMouvementCaisse.DEPENSE);
            }

            double totalDepenses = depenses.stream()
                    .mapToDouble(m -> m.getMontant() != null ? m.getMontant() : 0.0)
                    .sum();

            // Calculer le stock total de la boutique
            List<Stock> stocks = stockRepository.findByBoutiqueId(boutique.getId());
            int stockTotal = stocks.stream()
                    .mapToInt(s -> s.getStockActuel() != null ? s.getStockActuel() : 0)
                    .sum();

            ComptabiliteDTO.BoutiqueInfoDTO info = new ComptabiliteDTO.BoutiqueInfoDTO();
            info.setId(boutique.getId());
            info.setNom(boutique.getNomBoutique());
            info.setChiffreAffaires(chiffreAffaires);
            info.setNombreVentes(ventes.size());
            info.setTotalDepenses(totalDepenses);
            info.setNombreDepenses(depenses.size());
            info.setStockTotal(stockTotal);
            boutiquesInfo.add(info);
        }

        return boutiquesInfo;
    }

    /**
     * Liste toutes les boutiques de l'entreprise avec leurs informations de base
     */
    private List<ComptabiliteDTO.BoutiqueDisponibleDTO> listerBoutiquesDisponibles(Long entrepriseId) {
        List<Boutique> boutiques = boutiqueRepository.findByEntrepriseId(entrepriseId);
        return boutiques.stream().map(b -> {
            // Calculer le stock total de la boutique
            List<Stock> stocks = stockRepository.findByBoutiqueId(b.getId());
            int stockTotal = stocks.stream()
                    .mapToInt(s -> s.getStockActuel() != null ? s.getStockActuel() : 0)
                    .sum();
            
            ComptabiliteDTO.BoutiqueDisponibleDTO d = new ComptabiliteDTO.BoutiqueDisponibleDTO();
            d.setId(b.getId());
            d.setNom(b.getNomBoutique());
            d.setType(b.getTypeBoutique() != null ? b.getTypeBoutique().name() : null);
            d.setEmail(b.getEmail());
            d.setAdresse(b.getAdresse());
            d.setTelephone(b.getTelephone());
            d.setDateCreation(b.getCreatedAt());
            d.setStatut(b.isActif() ? "Actif" : "Inactif");
            d.setStockTotal(stockTotal);
            return d;
        }).collect(Collectors.toList());
    }

    /**
     * Récupère les caisses d'une boutique
     */
    private List<Caisse> getCaissesForBoutique(Long boutiqueId) {
        return caisseRepository.findByBoutiqueId(boutiqueId);
    }

    /**
     * Calcule les statistiques des clients
     */
    private ComptabiliteDTO.ClientsDTO calculerClients(Long entrepriseId) {
        List<Client> tousClients = clientRepository.findClientsByEntrepriseOrEntrepriseClient(entrepriseId);
        List<EntrepriseClient> tousEntreprisesClients = entrepriseClientRepository.findByEntrepriseId(entrepriseId);
        
        // Compter les clients actifs (ayant au moins une vente)
        Set<Long> clientsActifsIds = new HashSet<>();
        Set<Long> entrepriseClientsActifsIds = new HashSet<>();
        List<Vente> toutesVentes = venteRepository.findAllByEntrepriseId(entrepriseId);
        for (Vente vente : toutesVentes) {
            if (vente.getClient() != null) {
                clientsActifsIds.add(vente.getClient().getId());
            }
            if (vente.getEntrepriseClient() != null) {
                entrepriseClientsActifsIds.add(vente.getEntrepriseClient().getId());
            }
        }

        // Calculer le montant total acheté par les clients
        double montantTotalAchete = 0.0;
        List<Long> venteIds = toutesVentes.stream().map(Vente::getId).collect(Collectors.toList());
        Map<Long, Double> remboursementsMap = new HashMap<>();
        if (!venteIds.isEmpty()) {
            remboursementsMap = venteHistoriqueRepository.sumRemboursementsByVenteIds(venteIds)
                    .stream()
                    .collect(Collectors.toMap(
                            obj -> (Long) obj[0],
                            obj -> ((Number) obj[1]).doubleValue()
                    ));
        }

        for (Vente vente : toutesVentes) {
            double montantVente = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;
            double remboursements = remboursementsMap.getOrDefault(vente.getId(), 0.0);
            montantTotalAchete += montantVente - remboursements;
        }

        // Calculer les top 3 meilleurs clients
        List<ComptabiliteDTO.MeilleurClientDTO> meilleursClients = calculerTop3Clients(entrepriseId, toutesVentes, remboursementsMap);

        // Construire la liste de tous les clients (Client + EntrepriseClient)
        List<ComptabiliteDTO.ClientResumeDTO> clientsList = new ArrayList<>();
        for (Client c : tousClients) {
            ComptabiliteDTO.ClientResumeDTO cr = new ComptabiliteDTO.ClientResumeDTO();
            cr.setId(c.getId());
            cr.setNomComplet(c.getNomComplet());
            cr.setEmail(c.getEmail());
            cr.setTelephone(c.getTelephone());
            cr.setPhoto(c.getPhoto());
            cr.setAdresse(c.getAdresse());
            cr.setType("CLIENT");
            clientsList.add(cr);
        }
        for (EntrepriseClient ec : tousEntreprisesClients) {
            ComptabiliteDTO.ClientResumeDTO cr = new ComptabiliteDTO.ClientResumeDTO();
            cr.setId(ec.getId());
            cr.setNomComplet(ec.getNom());
            cr.setEmail(ec.getEmail());
            cr.setTelephone(ec.getTelephone());
            cr.setPhoto(null);
            cr.setType("ENTREPRISE_CLIENT");
            clientsList.add(cr);
        }

        // Assurer que tous les meilleurs clients figurent aussi dans la liste complète
        java.util.Set<String> signatures = clientsList.stream()
                .map(cl -> (cl.getType() + "|" + (cl.getId() != null ? cl.getId() : -1) + "|" +
                        (cl.getNomComplet() != null ? cl.getNomComplet() : "") + "|" +
                        (cl.getTelephone() != null ? cl.getTelephone() : "")))
                .collect(java.util.stream.Collectors.toSet());

        for (ComptabiliteDTO.MeilleurClientDTO mc : meilleursClients) {
            String sig = mc.getType() + "|" + (mc.getId() != null ? mc.getId() : -1) + "|" +
                    (mc.getNomComplet() != null ? mc.getNomComplet() : "") + "|" +
                    (mc.getTelephone() != null ? mc.getTelephone() : "");
            if (!signatures.contains(sig)) {
                ComptabiliteDTO.ClientResumeDTO cr = new ComptabiliteDTO.ClientResumeDTO();
                cr.setId(mc.getId());
                cr.setNomComplet(mc.getNomComplet());
                cr.setEmail(mc.getEmail());
                cr.setTelephone(mc.getTelephone());
                cr.setPhoto(mc.getPhoto());
                cr.setAdresse(mc.getAdresse());
                cr.setType(mc.getType());
                clientsList.add(cr);
                signatures.add(sig);
            }
        }

        // Inclure également les clients saisis en caisse (clientNom/clientNumero) absents du référentiel
        for (Vente v : toutesVentes) {
            if (v.getClient() != null || v.getEntrepriseClient() != null) continue;
            String nom = v.getClientNom();
            String tel = v.getClientNumero();
            if ((nom != null && !nom.isEmpty()) || (tel != null && !tel.isEmpty())) {
                String sig = "CLIENT|" + -1 + "|" + (nom != null ? nom : "") + "|" + (tel != null ? tel : "");
                if (!signatures.contains(sig)) {
                    ComptabiliteDTO.ClientResumeDTO cr = new ComptabiliteDTO.ClientResumeDTO();
                    cr.setId(null);
                    cr.setNomComplet(nom);
                    cr.setEmail(null);
                    cr.setTelephone(tel);
                    cr.setPhoto(null);
                    cr.setType("CLIENT");
                    clientsList.add(cr);
                    signatures.add(sig);
                }
            }
        }

        // Total clients = clients normaux + entreprises clients
        int totalClients = tousClients.size() + tousEntreprisesClients.size();
        int clientsActifsTotal = clientsActifsIds.size() + entrepriseClientsActifsIds.size();

        ComptabiliteDTO.ClientsDTO dto = new ComptabiliteDTO.ClientsDTO();
        dto.setNombreTotal(totalClients);
        dto.setActifs(clientsActifsTotal);
        dto.setMontantTotalAchete(montantTotalAchete);
        dto.setMeilleursClients(meilleursClients);
        dto.setClients(clientsList);
        return dto;
    }

    /**
     * Calcule les top 3 meilleurs clients par montant acheté
     */
    private List<ComptabiliteDTO.MeilleurClientDTO> calculerTop3Clients(Long entrepriseId, List<Vente> toutesVentes, Map<Long, Double> remboursementsMap) {
        // Calculer le montant acheté et nombre d'achats par client
        Map<String, ClientStats> statsParClient = new HashMap<>();
        
        for (Vente vente : toutesVentes) {
            double montantVente = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;
            double remboursements = remboursementsMap.getOrDefault(vente.getId(), 0.0);
            double montantNet = montantVente - remboursements;

            if (vente.getClient() != null) {
                Long clientId = vente.getClient().getId();
                String key = "CLIENT_" + clientId;
                statsParClient.computeIfAbsent(key, k -> new ClientStats(
                        vente.getClient().getId(),
                        vente.getClient().getNomComplet(),
                        vente.getClient().getEmail(),
                        vente.getClient().getTelephone(),
                        "CLIENT",
                        vente.getClient().getPhoto(),
                        vente.getClient().getAdresse()
                )).ajouterAchat(montantNet);
            } else if (vente.getEntrepriseClient() != null) {
                Long entrepriseClientId = vente.getEntrepriseClient().getId();
                String key = "ENTREPRISE_CLIENT_" + entrepriseClientId;
                statsParClient.computeIfAbsent(key, k -> new ClientStats(
                        vente.getEntrepriseClient().getId(),
                        vente.getEntrepriseClient().getNom(),
                        vente.getEntrepriseClient().getEmail(),
                        vente.getEntrepriseClient().getTelephone(),
                        "ENTREPRISE_CLIENT",
                        null,
                        vente.getEntrepriseClient().getAdresse()
                )).ajouterAchat(montantNet);
            }
        }

        // Trier par montant acheté décroissant et prendre le top 3
        List<ComptabiliteDTO.MeilleurClientDTO> top3 = statsParClient.values().stream()
                .sorted((a, b) -> Double.compare(b.montantAchete, a.montantAchete))
                .limit(3)
                .map(cs -> {
                    ComptabiliteDTO.MeilleurClientDTO d = new ComptabiliteDTO.MeilleurClientDTO();
                    d.setId(cs.id);
                    d.setNomComplet(cs.nomComplet);
                    d.setEmail(cs.email);
                    d.setTelephone(cs.telephone);
                    d.setPhoto(cs.photo);
                    d.setAdresse(cs.adresse);
                    d.setMontantAchete(cs.montantAchete);
                    d.setNombreAchats(cs.nombreAchats);
                    d.setType(cs.type);
                    return d;
                })
                .collect(Collectors.toList());

        return top3;
    }

    /**
     * Classe interne pour stocker les statistiques temporaires d'un client
     */
    private static class ClientStats {
        Long id;
        String nomComplet;
        String email;
        String telephone;
        String type;
        String photo;
        String adresse;
        double montantAchete = 0.0;
        int nombreAchats = 0;

        ClientStats(Long id, String nomComplet, String email, String telephone, String type, String photo, String adresse) {
            this.id = id;
            this.nomComplet = nomComplet;
            this.email = email;
            this.telephone = telephone;
            this.type = type;
            this.photo = photo;
            this.adresse = adresse;
        }

        void ajouterAchat(double montant) {
            this.montantAchete += montant;
            this.nombreAchats++;
        }
    }

    /**
     * Calcule les statistiques des vendeurs
     */
    private ComptabiliteDTO.VendeursDTO calculerVendeurs(Long entrepriseId) {
        List<User> tousVendeurs = usersRepository.findByEntrepriseId(entrepriseId);
        
        // Compter les vendeurs actifs (ayant au moins une vente)
        Set<Long> vendeursActifsIds = new HashSet<>();
        List<Vente> toutesVentes = venteRepository.findAllByEntrepriseId(entrepriseId);
        for (Vente vente : toutesVentes) {
            if (vente.getVendeur() != null) {
                vendeursActifsIds.add(vente.getVendeur().getId());
            }
        }

        // Calculer le chiffre d'affaires total généré par les vendeurs
        double chiffreAffairesTotal = calculerVentesNet(entrepriseId, null, null);

        // Calculer les top 3 meilleurs vendeurs
        List<ComptabiliteDTO.MeilleurVendeurDTO> meilleursVendeurs = calculerTop3Vendeurs(entrepriseId, toutesVentes);

        // Construire la liste de tous les vendeurs
        List<ComptabiliteDTO.VendeurResumeDTO> vendeursList = tousVendeurs.stream().map(u -> {
            ComptabiliteDTO.VendeurResumeDTO vr = new ComptabiliteDTO.VendeurResumeDTO();
            vr.setId(u.getId());
            vr.setNomComplet(u.getNomComplet());
            vr.setEmail(u.getEmail());
            vr.setTelephone(u.getPhone());
            vr.setPhoto(u.getPhoto());
            vr.setAdresse(null);
            return vr;
        }).collect(java.util.stream.Collectors.toList());

        ComptabiliteDTO.VendeursDTO dto = new ComptabiliteDTO.VendeursDTO();
        dto.setNombreTotal(tousVendeurs.size());
        dto.setActifs(vendeursActifsIds.size());
        dto.setChiffreAffairesTotal(chiffreAffairesTotal);
        dto.setMeilleursVendeurs(meilleursVendeurs);
        dto.setVendeurs(vendeursList);
        return dto;
    }

    /**
     * Calcule les top 3 meilleurs vendeurs par chiffre d'affaires
     */
    private List<ComptabiliteDTO.MeilleurVendeurDTO> calculerTop3Vendeurs(Long entrepriseId, List<Vente> toutesVentes) {
        // Récupérer tous les remboursements d'un coup
        List<Long> venteIds = toutesVentes.stream().map(Vente::getId).collect(Collectors.toList());
        Map<Long, Double> remboursementsMap = new HashMap<>();
        if (!venteIds.isEmpty()) {
            remboursementsMap = venteHistoriqueRepository.sumRemboursementsByVenteIds(venteIds)
                    .stream()
                    .collect(Collectors.toMap(
                            obj -> (Long) obj[0],
                            obj -> ((Number) obj[1]).doubleValue()
                    ));
        }

        // Calculer le CA et nombre de ventes par vendeur
        Map<Long, VendeurStats> statsParVendeur = new HashMap<>();
        for (Vente vente : toutesVentes) {
            if (vente.getVendeur() != null) {
                Long vendeurId = vente.getVendeur().getId();
                double montantVente = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;
                double remboursements = remboursementsMap.getOrDefault(vente.getId(), 0.0);
                double montantNet = montantVente - remboursements;

                statsParVendeur.computeIfAbsent(vendeurId, k -> new VendeurStats(
                        vente.getVendeur().getId(),
                        vente.getVendeur().getNomComplet(),
                        vente.getVendeur().getEmail(),
                        vente.getVendeur().getPhoto(),
                        null
                )).ajouterVente(montantNet);
            }
        }

        // Trier par chiffre d'affaires décroissant et prendre le top 3
        List<ComptabiliteDTO.MeilleurVendeurDTO> top3 = statsParVendeur.values().stream()
                .sorted((a, b) -> Double.compare(b.chiffreAffaires, a.chiffreAffaires))
                .limit(3)
                .map(vs -> {
                    ComptabiliteDTO.MeilleurVendeurDTO d = new ComptabiliteDTO.MeilleurVendeurDTO();
                    d.setId(vs.id);
                    d.setNomComplet(vs.nomComplet);
                    d.setEmail(vs.email);
                    d.setPhoto(vs.photo);
                    d.setAdresse(vs.adresse);
                    d.setChiffreAffaires(vs.chiffreAffaires);
                    d.setNombreVentes(vs.nombreVentes);
                    return d;
                })
                .collect(Collectors.toList());

        return top3;
    }

    /**
     * Classe interne pour stocker les statistiques temporaires d'un vendeur
     */
    private static class VendeurStats {
        Long id;
        String nomComplet;
        String email;
        String photo;
        String adresse;
        double chiffreAffaires = 0.0;
        int nombreVentes = 0;

        VendeurStats(Long id, String nomComplet, String email, String photo, String adresse) {
            this.id = id;
            this.nomComplet = nomComplet;
            this.email = email;
            this.photo = photo;
            this.adresse = adresse;
        }

        void ajouterVente(double montant) {
            this.chiffreAffaires += montant;
            this.nombreVentes++;
        }
    }

    /**
     * Calcule les statistiques d'activités
     */
    private ComptabiliteDTO.ActivitesDTO calculerActivites(Long entrepriseId) {
        List<Vente> toutesVentes = venteRepository.findAllByEntrepriseId(entrepriseId);
        List<FactureReelle> toutesFactures = factureReelleRepository.findByEntrepriseId(entrepriseId);
        List<MouvementCaisse> toutesDepenses = mouvementCaisseRepository
                .findByCaisse_Boutique_Entreprise_IdAndTypeMouvement(
                        entrepriseId, TypeMouvementCaisse.DEPENSE);
        List<DepenseGenerale> toutesDepensesGenerales = depenseGeneraleRepository.findByEntrepriseId(entrepriseId);

        // Activités du jour
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        
        List<Vente> ventesJour = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
                entrepriseId, startOfDay, endOfDay);
        List<FactureReelle> facturesJour = toutesFactures.stream()
                .filter(f -> f.getDateCreation() != null && f.getDateCreation().equals(today))
                .collect(Collectors.toList());
        List<MouvementCaisse> depensesJour = mouvementCaisseRepository
                .findByCaisse_Boutique_Entreprise_IdAndTypeMouvementAndDateMouvementBetween(
                        entrepriseId, TypeMouvementCaisse.DEPENSE, startOfDay, endOfDay);
        List<DepenseGenerale> depensesGeneralesJour = toutesDepensesGenerales.stream()
                .filter(d -> d.getDateCreation() != null && 
                        !d.getDateCreation().isBefore(startOfDay) && 
                        !d.getDateCreation().isAfter(endOfDay))
                .collect(Collectors.toList());

        int nombreTransactionsJour = ventesJour.size() + facturesJour.size() + depensesJour.size() + depensesGeneralesJour.size();

        return new ComptabiliteDTO.ActivitesDTO(
                toutesVentes.size(),
                toutesFactures.size(),
                toutesDepenses.size() + toutesDepensesGenerales.size(),
                nombreTransactionsJour
        );
    }

    /**
     * Crée une dépense générale pour l'entreprise.
     */
    @Transactional
    public DepenseGeneraleResponseDTO creerDepenseGenerale(DepenseGeneraleRequestDTO request, HttpServletRequest httpRequest) {
        User user = validateComptabilitePermission(httpRequest);
        validateDepenseRequest(request);
        
        CategorieDepense categorie = getOrCreateCategorie(request, user);
        SourceDepense source = parseEnum(SourceDepense.class, request.getSource(), "Source");
        Ordonnateur ordonnateur = parseEnum(Ordonnateur.class, request.getOrdonnateur(), "Ordonnateur");
        TypeCharge typeCharge = parseEnum(TypeCharge.class, request.getTypeCharge(), "TypeCharge");
        Produit produit = validateProduit(request.getProduitId(), user);
        Fournisseur fournisseur = validateFournisseur(request.getFournisseurId(), user);
        
        DepenseGenerale depense = createDepenseGenerale(request, user, categorie, source, ordonnateur, typeCharge, produit, fournisseur);
        depense = depenseGeneraleRepository.save(depense);
        
        return mapDepenseGeneraleToResponse(depense);
    }

    @Transactional(readOnly = true)
    public List<DepenseGeneraleResponseDTO> listerDepensesGenerales(HttpServletRequest httpRequest) {
        User user = validateComptabilitePermission(httpRequest);
        List<DepenseGenerale> depenses = depenseGeneraleRepository.findByEntrepriseIdOrderByDateCreationDesc(user.getEntreprise().getId());
        return depenses.stream()
                .map(this::mapDepenseGeneraleToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategorieDepenseDTO creerCategorieDepense(String nom, String description, HttpServletRequest httpRequest) {
        User user = validateComptabilitePermission(httpRequest);
        
        if (nom == null || nom.trim().isEmpty()) {
            throw new RuntimeException("Le nom de la catégorie est obligatoire.");
        }
        
        String nomTrim = nom.trim();
        if (categorieDepenseRepository.existsByNomAndEntrepriseId(nomTrim, user.getEntreprise().getId())) {
            throw new RuntimeException("Une catégorie avec ce nom existe déjà.");
        }

        CategorieDepense categorie = new CategorieDepense();
        categorie.setNom(nomTrim);
        categorie.setDescription(description != null ? description.trim() : null);
        categorie.setEntreprise(user.getEntreprise());
        
        categorie = categorieDepenseRepository.save(categorie);
        return mapCategorieDepenseToDTO(categorie);
    }

    @Transactional(readOnly = true)
    public List<CategorieDepenseDTO> listerCategoriesDepense(HttpServletRequest httpRequest) {
        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        
        if (user.getEntreprise() == null) {
            throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
        }

        List<CategorieDepense> categories = categorieDepenseRepository.findByEntrepriseId(user.getEntreprise().getId());
        return categories.stream()
                .map(this::mapCategorieDepenseToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategorieEntreeDTO creerCategorieEntree(String nom, String description, HttpServletRequest httpRequest) {
        User user = validateComptabilitePermission(httpRequest);
        
        if (nom == null || nom.trim().isEmpty()) {
            throw new RuntimeException("Le nom de la catégorie est obligatoire.");
        }
        
        String nomTrim = nom.trim();
        Categorie categorieExistante = categorieRepository.findByNomAndEntrepriseId(nomTrim, user.getEntreprise().getId());
        if (categorieExistante != null) {
            // Mettre à jour la description si elle est fournie
            if (description != null && !description.trim().isEmpty()) {
                categorieExistante.setDescription(description.trim());
                categorieExistante = categorieRepository.save(categorieExistante);
            }
            return mapCategorieToCategorieEntreeDTO(categorieExistante);
        }

        Categorie categorie = new Categorie();
        categorie.setNom(nomTrim);
        categorie.setDescription(description != null ? description.trim() : null);
        categorie.setEntreprise(user.getEntreprise());
        categorie.setCreatedAt(LocalDateTime.now());
        categorie.setProduitCount(0);
        categorie.setOrigineCreation("COMPTABILITE");
        
        categorie = categorieRepository.save(categorie);
        return mapCategorieToCategorieEntreeDTO(categorie);
    }

    // @Transactional(readOnly = true)
    // public List<CategorieResponseDTO> listerCategoriesEntree(HttpServletRequest httpRequest) {
    //     // Utiliser CategorieService pour obtenir les catégories avec CategorieResponseDTO
    //     return categorieService.getAllCategoriesWithProduitCount(httpRequest);
    // }

    // ========== Méthodes utilitaires privées pour les dépenses générales ==========

    /**
     * Valide que l'utilisateur a la permission COMPTABILITE
     */
    private User validateComptabilitePermission(HttpServletRequest httpRequest) {
        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        
        if (user.getEntreprise() == null) {
            throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
        }

        boolean isComptable = user.getRole() != null && user.getRole().getName() == RoleType.COMPTABLE;
        boolean hasPermission = user.getRole() != null && user.getRole().hasPermission(PermissionType.COMPTABILITE);

        if (!isComptable && !hasPermission) {
            throw new RuntimeException("Seul un comptable ou un utilisateur disposant de la permission COMPTABILITE peut effectuer cette opération.");
        }
        
        return user;
    }

    /**
     * Valide les champs obligatoires d'une dépense
     */
    private void validateDepenseRequest(DepenseGeneraleRequestDTO request) {
        if (request.getDesignation() == null || request.getDesignation().trim().isEmpty()) {
            throw new RuntimeException("La désignation est obligatoire.");
        }
        if (request.getPrixUnitaire() == null || request.getPrixUnitaire() <= 0) {
            throw new RuntimeException("Le prix unitaire doit être supérieur à zéro.");
        }
        if (request.getQuantite() == null || request.getQuantite() <= 0) {
            throw new RuntimeException("La quantité doit être supérieure à zéro.");
        }
        if (request.getSource() == null || request.getSource().trim().isEmpty()) {
            throw new RuntimeException("La source est obligatoire.");
        }
        if (request.getOrdonnateur() == null || request.getOrdonnateur().trim().isEmpty()) {
            throw new RuntimeException("L'ordonnateur est obligatoire.");
        }
        if (request.getTypeCharge() == null || request.getTypeCharge().trim().isEmpty()) {
            throw new RuntimeException("Le type de charge est obligatoire.");
        }
    }

    /**
     * Parse un enum de manière générique avec gestion d'erreur
     */
    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(fieldName + " est obligatoire.");
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // Construire la liste des valeurs valides
            List<String> validValuesList = new ArrayList<>();
            for (T enumValue : enumClass.getEnumConstants()) {
                validValuesList.add(enumValue.name());
            }
            String validValues = String.join(", ", validValuesList);
            throw new RuntimeException(fieldName + " invalide : " + value + ". Valeurs acceptées : " + validValues);
        }
    }

    /**
     * Récupère ou crée une catégorie de dépense
     */
    private CategorieDepense getOrCreateCategorie(DepenseGeneraleRequestDTO request, User user) {
        if (request.getCategorieId() != null) {
            return categorieDepenseRepository.findByIdAndEntrepriseId(request.getCategorieId(), user.getEntreprise().getId())
                    .orElseThrow(() -> new RuntimeException("Catégorie de dépense non trouvée."));
        }
        
        if (request.getNouvelleCategorieNom() != null && !request.getNouvelleCategorieNom().trim().isEmpty()) {
            String nomCategorie = request.getNouvelleCategorieNom().trim();
            CategorieDepense categorie = categorieDepenseRepository.findByNomAndEntrepriseId(nomCategorie, user.getEntreprise().getId())
                    .orElse(null);
            
            if (categorie == null) {
                categorie = new CategorieDepense();
                categorie.setNom(nomCategorie);
                categorie.setEntreprise(user.getEntreprise());
                categorie = categorieDepenseRepository.save(categorie);
            }
            return categorie;
        }
        
        return null;
    }

    /**
     * Valide et récupère un produit s'il est fourni
     */
    private Produit validateProduit(Long produitId, User user) {
        if (produitId == null) {
            return null;
        }
        
        Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new RuntimeException("Produit non trouvé."));
        
        if (produit.getBoutique() == null || !produit.getBoutique().getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Le produit n'appartient pas à votre entreprise.");
        }
        
        return produit;
    }

    /**
     * Valide et récupère un fournisseur s'il est fourni
     */
    private Fournisseur validateFournisseur(Long fournisseurId, User user) {
        if (fournisseurId == null) {
            return null;
        }
        
        Fournisseur fournisseur = fournisseurRepository.findById(fournisseurId)
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé."));
        
        if (fournisseur.getEntreprise() == null || !fournisseur.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Le fournisseur n'appartient pas à votre entreprise.");
        }
        
        return fournisseur;
    }

    /**
     * Valide et récupère un responsable
     */
    private User validateResponsable(Long responsableId, User user) {
        if (responsableId == null) {
            throw new RuntimeException("Le responsable est obligatoire.");
        }
        
        User responsable = usersRepository.findById(responsableId)
                .orElseThrow(() -> new RuntimeException("Responsable non trouvé."));
        
        if (responsable.getEntreprise() == null || !responsable.getEntreprise().getId().equals(user.getEntreprise().getId())) {
            throw new RuntimeException("Le responsable n'appartient pas à votre entreprise.");
        }
        
        return responsable;
    }

    /**
     * Génère un numéro unique pour une dépense générale au format "DP: 001-11-2025"
     * Le compteur est réinitialisé chaque mois
     */
    private String genererNumeroDepense(Long entrepriseId) {
        LocalDate currentDate = LocalDate.now();
        int month = currentDate.getMonthValue();
        int year = currentDate.getYear();
        
        // Récupérer les dépenses du mois en cours
        List<DepenseGenerale> depensesDuMois = depenseGeneraleRepository
                .findByEntrepriseIdAndMonthAndYear(entrepriseId, month, year);
        
        long newIndex = 1;
        
        if (!depensesDuMois.isEmpty()) {
            String lastNumero = depensesDuMois.get(0).getNumero();
            
            if (lastNumero != null && !lastNumero.isEmpty()) {
                Pattern pattern = Pattern.compile("DP:\\s*(\\d+)-");
                Matcher matcher = pattern.matcher(lastNumero);
                
                if (matcher.find()) {
                    try {
                        newIndex = Long.parseLong(matcher.group(1)) + 1;
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Impossible de parser l'index numérique dans le numéro : " + lastNumero, e);
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
        
        // Formater le numéro : "DP: 001-11-2025"
        String indexFormate = String.format("%03d", newIndex);
        String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("MM-yyyy"));
        
        return "DP: " + indexFormate + "-" + formattedDate;
    }

    /**
     * Génère un numéro unique pour une fermeture de caisse
     * Format: "FERM-001-MM-YYYY" avec compteur qui se réinitialise chaque mois
     */
    private String genererNumeroFermeture(Long entrepriseId, LocalDateTime dateFermeture) {
        if (dateFermeture == null) {
            dateFermeture = LocalDateTime.now();
        }
        
        LocalDate dateFermetureLocal = dateFermeture.toLocalDate();
        int month = dateFermetureLocal.getMonthValue();
        int year = dateFermetureLocal.getYear();
        
        // Récupérer toutes les caisses fermées du mois pour cette entreprise
        List<Caisse> caissesFermeesDuMois = caisseRepository.findByEntrepriseIdAndStatut(entrepriseId, StatutCaisse.FERMEE)
                .stream()
                .filter(c -> c.getDateFermeture() != null 
                        && c.getDateFermeture().getMonthValue() == month 
                        && c.getDateFermeture().getYear() == year)
                .sorted((a, b) -> {
                    if (a.getDateFermeture() == null && b.getDateFermeture() == null) return 0;
                    if (a.getDateFermeture() == null) return 1;
                    if (b.getDateFermeture() == null) return -1;
                    return b.getDateFermeture().compareTo(a.getDateFermeture());
                })
                .collect(Collectors.toList());
        
        long newIndex = caissesFermeesDuMois.size() + 1;
        
        // Formater le numéro : "FERM-001-12-2025"
        String indexFormate = String.format("%03d", newIndex);
        String formattedDate = dateFermetureLocal.format(DateTimeFormatter.ofPattern("MM-yyyy"));
        
        return "C-" + indexFormate + "-" + formattedDate;
    }

    /**
     * Crée une entité DepenseGenerale à partir du DTO
     */
    private DepenseGenerale createDepenseGenerale(
            DepenseGeneraleRequestDTO request,
            User user,
            CategorieDepense categorie,
            SourceDepense source,
            Ordonnateur ordonnateur,
            TypeCharge typeCharge,
            Produit produit,
            Fournisseur fournisseur) {
        
        DepenseGenerale depense = new DepenseGenerale();
        
        // Générer le numéro unique avant de sauvegarder
        String numero = genererNumeroDepense(user.getEntreprise().getId());
        depense.setNumero(numero);
        
        depense.setDesignation(request.getDesignation().trim());
        depense.setCategorie(categorie);
        depense.setPrixUnitaire(request.getPrixUnitaire());
        depense.setQuantite(request.getQuantite());
        depense.setMontant(request.getPrixUnitaire() * request.getQuantite());
        depense.setSource(source);
        depense.setOrdonnateur(ordonnateur);
        depense.setNumeroCheque(request.getNumeroCheque() != null ? request.getNumeroCheque().trim() : null);
        depense.setTypeCharge(typeCharge);
        depense.setProduit(produit);
        depense.setFournisseur(fournisseur);
        depense.setPieceJointe(request.getPieceJointe());
        depense.setEntreprise(user.getEntreprise());
        depense.setCreePar(user);
        
        return depense;
    }

    /**
     * Mappe une DepenseGenerale vers DepenseGeneraleResponseDTO
     */
    private DepenseGeneraleResponseDTO mapDepenseGeneraleToResponse(DepenseGenerale depense) {
        DepenseGeneraleResponseDTO dto = new DepenseGeneraleResponseDTO();
        dto.setId(depense.getId());
        dto.setNumero(depense.getNumero());
        dto.setDesignation(depense.getDesignation());
        if (depense.getCategorie() != null) {
            dto.setCategorieId(depense.getCategorie().getId());
            dto.setCategorieNom(depense.getCategorie().getNom());
        }
        dto.setPrixUnitaire(depense.getPrixUnitaire());
        dto.setQuantite(depense.getQuantite());
        dto.setMontant(depense.getMontant());
        dto.setSource(depense.getSource() != null ? depense.getSource().name() : null);
        dto.setOrdonnateur(depense.getOrdonnateur() != null ? depense.getOrdonnateur().name() : null);
        dto.setNumeroCheque(depense.getNumeroCheque());
        dto.setTypeCharge(depense.getTypeCharge() != null ? depense.getTypeCharge().name() : null);
        if (depense.getProduit() != null) {
            dto.setProduitId(depense.getProduit().getId());
            dto.setProduitNom(depense.getProduit().getNom());
        }
        if (depense.getFournisseur() != null) {
            dto.setFournisseurId(depense.getFournisseur().getId());
            dto.setFournisseurNom(depense.getFournisseur().getNomComplet() != null ? 
                    depense.getFournisseur().getNomComplet() : depense.getFournisseur().getNomSociete());
        }
        dto.setPieceJointe(depense.getPieceJointe());
        if (depense.getEntreprise() != null) {
            dto.setEntrepriseId(depense.getEntreprise().getId());
            dto.setEntrepriseNom(depense.getEntreprise().getNomEntreprise());
        }
        if (depense.getCreePar() != null) {
            dto.setCreeParId(depense.getCreePar().getId());
            dto.setCreeParNom(depense.getCreePar().getNomComplet());
            dto.setCreeParEmail(depense.getCreePar().getEmail());
        }
        dto.setDateCreation(depense.getDateCreation());
        dto.setTypeTransaction("SORTIE");
        dto.setOrigine("COMPTABILITE"); // Les dépenses générales viennent de la comptabilité
        return dto;
    }

    /**
     * Mappe une CategorieDepense vers CategorieDepenseDTO
     */
    private CategorieDepenseDTO mapCategorieDepenseToDTO(CategorieDepense categorie) {
        CategorieDepenseDTO dto = new CategorieDepenseDTO();
        dto.setId(categorie.getId());
        dto.setNom(categorie.getNom());
        dto.setDescription(categorie.getDescription());
        if (categorie.getEntreprise() != null) {
            dto.setEntrepriseId(categorie.getEntreprise().getId());
        }
        dto.setCreatedAt(categorie.getCreatedAt());
        return dto;
    }

    /**
     * Crée une entrée générale pour l'entreprise.
     */
    @Transactional
    public EntreeGeneraleResponseDTO creerEntreeGenerale(EntreeGeneraleRequestDTO request, HttpServletRequest httpRequest) {
        User user = validateComptabilitePermission(httpRequest);
        validateEntreeRequest(request);
        
        Categorie categorie = getOrCreateCategorie(request, user);
        SourceDepense source = parseEnum(SourceDepense.class, request.getSource(), "Source");
        
        ModePaiement modeEntree = null;
        String numeroModeEntree = null;
        
        if (source == SourceDepense.BANQUE) {
            if (request.getModeEntree() == null || request.getModeEntree().trim().isEmpty()) {
                throw new RuntimeException("Le mode d'entrée est obligatoire lorsque la source est BANQUE.");
            }
            if (request.getNumeroModeEntree() == null || request.getNumeroModeEntree().trim().isEmpty()) {
                throw new RuntimeException("Le numéro du mode d'entrée est obligatoire lorsque la source est BANQUE.");
            }
            
            try {
                modeEntree = ModePaiement.valueOf(request.getModeEntree().trim().toUpperCase());
                if (modeEntree != ModePaiement.CHEQUE && modeEntree != ModePaiement.VIREMENT && modeEntree != ModePaiement.RETRAIT) {
                    throw new RuntimeException("Mode d'entrée invalide pour BANQUE : " + request.getModeEntree() + ". Valeurs acceptées : CHEQUE, VIREMENT, RETRAIT");
                }
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Mode d'entrée invalide : " + request.getModeEntree() + ". Valeurs acceptées : CHEQUE, VIREMENT, RETRAIT");
            }
            
            numeroModeEntree = request.getNumeroModeEntree().trim();
        } else {
            if (request.getModeEntree() != null && !request.getModeEntree().trim().isEmpty()) {
                throw new RuntimeException("Le mode d'entrée ne doit être fourni que lorsque la source est BANQUE.");
            }
            if (request.getNumeroModeEntree() != null && !request.getNumeroModeEntree().trim().isEmpty()) {
                throw new RuntimeException("Le numéro du mode d'entrée ne doit être fourni que lorsque la source est BANQUE.");
            }
        }
        
        User responsable = validateResponsable(request.getResponsableId(), user);
        
        EntreeGenerale entree = createEntreeGenerale(request, user, responsable, categorie, source, modeEntree, numeroModeEntree);
        entree = entreeGeneraleRepository.save(entree);
        
        return mapEntreeGeneraleToResponse(entree);
    }

    @Transactional(readOnly = true)
    public List<EntreeGeneraleResponseDTO> listerEntreesGenerales(HttpServletRequest httpRequest) {
        User user = validateComptabilitePermission(httpRequest);
        List<EntreeGenerale> entrees = entreeGeneraleRepository.findByEntrepriseIdOrderByDateCreationDesc(user.getEntreprise().getId());
        return entrees.stream()
                .map(this::mapEntreeGeneraleToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Valide les champs obligatoires d'une entrée
     */
    private void validateEntreeRequest(EntreeGeneraleRequestDTO request) {
        if (request.getDesignation() == null || request.getDesignation().trim().isEmpty()) {
            throw new RuntimeException("La désignation est obligatoire.");
        }
        if (request.getPrixUnitaire() == null || request.getPrixUnitaire() <= 0) {
            throw new RuntimeException("Le prix unitaire doit être supérieur à zéro.");
        }
        if (request.getQuantite() == null || request.getQuantite() <= 0) {
            throw new RuntimeException("La quantité doit être supérieure à zéro.");
        }
        if (request.getSource() == null || request.getSource().trim().isEmpty()) {
            throw new RuntimeException("La source est obligatoire.");
        }
        if (request.getResponsableId() == null) {
            throw new RuntimeException("Le responsable est obligatoire.");
        }
    }

    /**
     * Récupère ou crée une catégorie de produit pour les entrées générales
     */
    private Categorie getOrCreateCategorie(EntreeGeneraleRequestDTO request, User user) {
        if (request.getCategorieId() != null) {
            return categorieRepository.findByIdAndEntrepriseId(request.getCategorieId(), user.getEntreprise().getId())
                    .orElseThrow(() -> new RuntimeException("Catégorie non trouvée."));
        }
        
        if (request.getNouvelleCategorieNom() != null && !request.getNouvelleCategorieNom().trim().isEmpty()) {
            String nomCategorie = request.getNouvelleCategorieNom().trim();
            Categorie categorie = categorieRepository.findByNomAndEntrepriseId(nomCategorie, user.getEntreprise().getId());
            
            if (categorie == null) {
                categorie = new Categorie();
                categorie.setNom(nomCategorie);
                categorie.setEntreprise(user.getEntreprise());
                categorie.setCreatedAt(LocalDateTime.now());
                categorie.setProduitCount(0);
                categorie.setOrigineCreation("COMPTABILITE");
                categorie = categorieRepository.save(categorie);
            }
            return categorie;
        }
        
        return null;
    }

    /**
     * Génère un numéro unique pour une entrée générale au format "EN: 001-11-2025"
     */
    private String genererNumeroEntree(Long entrepriseId) {
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
                        throw new RuntimeException("Impossible de parser l'index numérique dans le numéro : " + lastNumero, e);
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

    /**
     * Crée une entité EntreeGenerale à partir du DTO
     */
    private EntreeGenerale createEntreeGenerale(
            EntreeGeneraleRequestDTO request,
            User user,
            User responsable,
            Categorie categorie,
            SourceDepense source,
            ModePaiement modeEntree,
            String numeroModeEntree) {
        
        EntreeGenerale entree = new EntreeGenerale();
        
        String numero = genererNumeroEntree(user.getEntreprise().getId());
        entree.setNumero(numero);
        
        entree.setDesignation(request.getDesignation().trim());
        entree.setCategorie(categorie);
        entree.setPrixUnitaire(request.getPrixUnitaire());
        entree.setQuantite(request.getQuantite());
        entree.setMontant(request.getPrixUnitaire() * request.getQuantite());
        entree.setSource(source);
        entree.setModeEntree(modeEntree);
        entree.setNumeroModeEntree(numeroModeEntree);
        entree.setPieceJointe(request.getPieceJointe());
        entree.setEntreprise(user.getEntreprise());
        entree.setCreePar(user);
        entree.setResponsable(responsable);
        
        return entree;
    }

    /**
     * Mappe une EntreeGenerale vers EntreeGeneraleResponseDTO
     */
    private EntreeGeneraleResponseDTO mapEntreeGeneraleToResponse(EntreeGenerale entree) {
        EntreeGeneraleResponseDTO dto = new EntreeGeneraleResponseDTO();
        dto.setId(entree.getId());
        dto.setNumero(entree.getNumero());
        dto.setDesignation(entree.getDesignation());
        if (entree.getCategorie() != null) {
            dto.setCategorieId(entree.getCategorie().getId());
            dto.setCategorieNom(entree.getCategorie().getNom());
            dto.setCategorieDescription(entree.getCategorie().getDescription());
        }
        dto.setPrixUnitaire(entree.getPrixUnitaire());
        dto.setQuantite(entree.getQuantite());
        dto.setMontant(entree.getMontant());
        dto.setSource(entree.getSource() != null ? entree.getSource().name() : null);
        dto.setModeEntree(entree.getModeEntree() != null ? entree.getModeEntree().name() : null);
        dto.setNumeroModeEntree(entree.getNumeroModeEntree());
        dto.setPieceJointe(entree.getPieceJointe());
        if (entree.getEntreprise() != null) {
            dto.setEntrepriseId(entree.getEntreprise().getId());
            dto.setEntrepriseNom(entree.getEntreprise().getNomEntreprise());
        }
        if (entree.getCreePar() != null) {
            dto.setCreeParId(entree.getCreePar().getId());
            dto.setCreeParNom(entree.getCreePar().getNomComplet());
            dto.setCreeParEmail(entree.getCreePar().getEmail());
        }
        if (entree.getResponsable() != null) {
            dto.setResponsableId(entree.getResponsable().getId());
            dto.setResponsableNom(entree.getResponsable().getNomComplet());
            dto.setResponsableEmail(entree.getResponsable().getEmail());
        }
        dto.setDateCreation(entree.getDateCreation());
        dto.setTypeTransaction("ENTREE");
        dto.setOrigine("COMPTABILITE"); // Les entrées générales viennent de la comptabilité
        return dto;
    }

    /**
     * Mappe une Categorie (produit) vers CategorieEntreeDTO pour les entrées générales
     */
    private CategorieEntreeDTO mapCategorieToCategorieEntreeDTO(Categorie categorie) {
        CategorieEntreeDTO dto = new CategorieEntreeDTO();
        dto.setId(categorie.getId());
        dto.setNom(categorie.getNom());
        dto.setDescription(categorie.getDescription());
        dto.setOrigineCreation(categorie.getOrigineCreation());
        if (categorie.getEntreprise() != null) {
            dto.setEntrepriseId(categorie.getEntreprise().getId());
        }
        dto.setCreatedAt(categorie.getCreatedAt());
        return dto;
    }

    @Transactional(readOnly = true)
    public ComptabiliteCompleteDTO getComptabiliteComplete(HttpServletRequest httpRequest) {
        User user = validateComptabilitePermission(httpRequest);
        Long entrepriseId = user.getEntreprise().getId();

        List<DepenseGeneraleResponseDTO> depensesGenerales = listerDepensesGenerales(httpRequest);
        List<CategorieDepenseDTO> categoriesDepense = listerCategoriesDepense(httpRequest);
        List<CategorieResponseDTO> categoriesEntree = categorieService.getAllCategoriesWithProduitCount(httpRequest);
        List<EntreeGeneraleResponseDTO> entreesGenerales = listerEntreesGenerales(httpRequest);
        TransactionSummaryDTO transactionSummary = transactionSummaryService.getTransactionSummary(httpRequest);

        List<Vente> ventesCaissesFermees = venteRepository.findByEntrepriseIdAndCaisseFermee(entrepriseId);
        List<VenteResponse> ventesCaissesFermeesDTO = ventesCaissesFermees.stream()
                .map(this::mapVenteToResponse)
                .collect(Collectors.toList());

        List<Paiement> paiements = paiementRepository.findByEntrepriseId(entrepriseId);
        List<PaiementDTO> paiementsDTO = paiements.stream()
                .map(p -> {
                    PaiementDTO dto = new PaiementDTO(p);
                    dto.setTypeTransaction("ENTREE");
                    return dto;
                })
                .collect(Collectors.toList());

        List<TransfertFondsResponseDTO> transfertsFonds = transfertFondsService.listerTransferts(httpRequest);

        ComptabiliteCompleteDTO result = new ComptabiliteCompleteDTO();
        result.setDepensesGenerales(depensesGenerales);
        result.setCategoriesDepense(categoriesDepense);
        result.setCategoriesEntree(categoriesEntree);
        result.setEntreesGenerales(entreesGenerales);
        result.setTransactionSummary(transactionSummary);
        result.setVentesCaissesFermees(ventesCaissesFermeesDTO);
        result.setPaiementsFactures(paiementsDTO);
        result.setTransfertsFonds(transfertsFonds);

        return result;
    }

    @Transactional(readOnly = true)
    public ComptabiliteCompletePaginatedDTO getComptabiliteCompletePaginated(HttpServletRequest httpRequest, int page, int size) {
        // Validation des paramètres de pagination
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100; 

        User user = validateComptabilitePermission(httpRequest);
        Long entrepriseId = user.getEntreprise().getId();

        // Charger les catégories et le résumé (petites données, pas besoin de pagination)
        List<CategorieDepenseDTO> categoriesDepense = listerCategoriesDepense(httpRequest);
        List<CategorieResponseDTO> categoriesEntree = categorieService.getAllCategoriesWithProduitCount(httpRequest);
        TransactionSummaryDTO transactionSummary = transactionSummaryService.getTransactionSummary(httpRequest);
       
        int limitParType = Math.max((page + 1) * size, 1000);

        // Charger les transactions avec limite
        List<DepenseGeneraleResponseDTO> depensesGenerales = listerDepensesGenerales(httpRequest).stream()
                .sorted((a, b) -> {
                    LocalDateTime dateA = a.getDateCreation();
                    LocalDateTime dateB = b.getDateCreation();
                    if (dateA == null && dateB == null) return 0;
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA);
                })
                .limit(limitParType)
                .collect(Collectors.toList());

        List<EntreeGeneraleResponseDTO> entreesGenerales = listerEntreesGenerales(httpRequest).stream()
                .sorted((a, b) -> {
                    LocalDateTime dateA = a.getDateCreation();
                    LocalDateTime dateB = b.getDateCreation();
                    if (dateA == null && dateB == null) return 0;
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA);
                })
                .limit(limitParType)
                .collect(Collectors.toList());

        // Récupérer toutes les ventes des caisses fermées
        List<Vente> toutesVentesCaissesFermees = venteRepository.findByEntrepriseIdAndCaisseFermee(entrepriseId);
        
        // Grouper les ventes par caisse fermée
        Map<Long, List<Vente>> ventesParCaisse = toutesVentesCaissesFermees.stream()
                .filter(v -> v.getCaisse() != null)
                .collect(Collectors.groupingBy(v -> v.getCaisse().getId()));
        
        // Récupérer toutes les caisses fermées
        List<Caisse> caissesFermees = caisseRepository.findByEntrepriseIdAndStatut(entrepriseId, StatutCaisse.FERMEE);
        
        // Créer les DTOs de fermeture de caisse avec leurs ventes
        List<FermetureCaisseResponseDTO> fermeturesCaissesDTO = caissesFermees.stream()
                .filter(c -> ventesParCaisse.containsKey(c.getId()) && !ventesParCaisse.get(c.getId()).isEmpty())
                .sorted((a, b) -> {
                    LocalDateTime dateA = a.getDateFermeture();
                    LocalDateTime dateB = b.getDateFermeture();
                    if (dateA == null && dateB == null) return 0;
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA); // Plus récent en premier
                })
                .limit(limitParType)
                .map(caisse -> {
                    List<Vente> ventesDeLaCaisse = ventesParCaisse.get(caisse.getId());
                    return mapCaisseFermeeToResponse(caisse, ventesDeLaCaisse, entrepriseId);
                })
                .collect(Collectors.toList());

        List<Paiement> paiements = paiementRepository.findByEntrepriseId(entrepriseId).stream()
                .sorted((a, b) -> {
                    LocalDateTime dateA = a.getDatePaiement();
                    LocalDateTime dateB = b.getDatePaiement();
                    if (dateA == null && dateB == null) return 0;
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA);
                })
                .limit(limitParType)
                .collect(Collectors.toList());
        List<PaiementDTO> paiementsDTO = paiements.stream()
                .map(p -> {
                    PaiementDTO dto = new PaiementDTO(p);
                    dto.setTypeTransaction("PAIEMENT_FACTURE");
                    
                    // Construire la description avec les informations de la facture
                    if (p.getFactureReelle() != null) {
                        FactureReelle facture = p.getFactureReelle();
                        
                        // Charger explicitement les lignes de facture si elles ne sont pas déjà chargées
                        if (facture.getLignesFacture() != null) {
                            org.hibernate.Hibernate.initialize(facture.getLignesFacture());
                        }
                        
                        // Définir le numéro de facture
                        dto.setNumeroFacture(facture.getNumeroFacture());
                        
                        // Définir l'objet (description de la facture)
                        String objetFacture = facture.getDescription() != null && !facture.getDescription().trim().isEmpty() 
                            ? facture.getDescription() 
                            : null;
                        dto.setObjet(objetFacture);
                        
                        // Mapper les lignes de facture
                        if (facture.getLignesFacture() != null && !facture.getLignesFacture().isEmpty()) {
                            List<LigneFactureDTO> lignesDTO = facture.getLignesFacture().stream()
                                    .map(LigneFactureDTO::new)
                                    .collect(Collectors.toList());
                            dto.setLignesFacture(lignesDTO);
                        } else {
                            dto.setLignesFacture(new ArrayList<>());
                        }
                        
                        // Construire la description principale
                        String description = "Paiement facture " + facture.getNumeroFacture();
                        
                        // Ajouter la description de la facture si elle existe
                        if (objetFacture != null) {
                            description += " - " + objetFacture;
                        }
                        
                        // Ajouter le montant restant si la facture est partiellement payée
                        String statutFacture = facture.getStatutPaiement() != null ? facture.getStatutPaiement().name() : "INCONNU";
                        if ("PARTIELLEMENT_PAYEE".equals(statutFacture)) {
                            java.math.BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(facture.getId());
                            if (totalPaye == null) totalPaye = java.math.BigDecimal.ZERO;
                            double montantRestant = facture.getTotalFacture() - totalPaye.doubleValue();
                            description += " (Montant restant: " + montantRestant + ")";
                        }
                        
                        dto.setDescription(description);
                        dto.setStatut(statutFacture);
                        dto.setOrigine("FACTURE"); // Les paiements viennent des factures réelles
                    } else {
                        dto.setDescription("Paiement sans facture associée");
                        dto.setObjet(null);
                        dto.setNumeroFacture(null);
                        dto.setStatut("INCONNU");
                        dto.setOrigine("FACTURE"); // Par défaut même sans facture associée
                        dto.setLignesFacture(new ArrayList<>());
                    }
                    
                    return dto;
                })
                .collect(Collectors.toList());

        // Récupérer les transferts de fonds et créer deux transactions par transfert (SORTIE et ENTREE)
        List<TransfertFondsResponseDTO> transfertsFondsBruts = transfertFondsService.listerTransferts(httpRequest).stream()
                .sorted((a, b) -> {
                    LocalDateTime dateA = a.getDateTransfert();
                    LocalDateTime dateB = b.getDateTransfert();
                    if (dateA == null && dateB == null) return 0;
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA);
                })
                .limit(limitParType)
                .collect(Collectors.toList());
        
        // Créer deux transactions par transfert : une SORTIE (source) et une ENTREE (destination)
        List<TransfertFondsResponseDTO> transfertsFonds = new ArrayList<>();
        for (TransfertFondsResponseDTO transfert : transfertsFondsBruts) {
            // Transaction SORTIE (depuis la source)
            TransfertFondsResponseDTO sortie = creerTransactionTransfert(transfert, "SORTIE", transfert.getDe());
            transfertsFonds.add(sortie);
            
            // Transaction ENTREE (vers la destination)
            TransfertFondsResponseDTO entree = creerTransactionTransfert(transfert, "ENTREE", transfert.getVers());
            transfertsFonds.add(entree);
        }

        // Combiner toutes les transactions dans une seule liste
        List<Object> toutesTransactions = new ArrayList<>();
        toutesTransactions.addAll(depensesGenerales);
        toutesTransactions.addAll(entreesGenerales);
        toutesTransactions.addAll(fermeturesCaissesDTO); // Utiliser les fermetures regroupées au lieu des ventes individuelles
        toutesTransactions.addAll(paiementsDTO);
        toutesTransactions.addAll(transfertsFonds);

        // Trier par date (plus récent en premier) - maintenant sur un ensemble limité
        toutesTransactions.sort((a, b) -> {
            LocalDateTime dateA = getDateFromTransaction(a);
            LocalDateTime dateB = getDateFromTransaction(b);
            if (dateA == null && dateB == null) return 0;
            if (dateA == null) return 1;
            if (dateB == null) return -1;
            return dateB.compareTo(dateA); // Tri décroissant
        });

        // Pagination manuelle
        int totalElements = toutesTransactions.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int start = page * size;
        int end = Math.min(start + size, totalElements);
        List<Object> transactionsPage = start < totalElements ? toutesTransactions.subList(start, end) : new ArrayList<>();

        ComptabiliteCompletePaginatedDTO result = new ComptabiliteCompletePaginatedDTO();
        result.setTransactions(transactionsPage);
        result.setCategoriesDepense(categoriesDepense);
        result.setCategoriesEntree(categoriesEntree);
        result.setTransactionSummary(transactionSummary);
        result.setPageNumber(page);
        result.setPageSize(size);
        result.setTotalElements(totalElements);
        result.setTotalPages(totalPages);
        result.setHasNext(page < totalPages - 1);
        result.setHasPrevious(page > 0);
        result.setFirst(page == 0);
        result.setLast(page >= totalPages - 1);

        return result;
    }

    private LocalDateTime getDateFromTransaction(Object transaction) {
        if (transaction instanceof DepenseGeneraleResponseDTO) {
            DepenseGeneraleResponseDTO dto = (DepenseGeneraleResponseDTO) transaction;
            return dto.getDateCreation() != null ? dto.getDateCreation() : null;
        } else if (transaction instanceof EntreeGeneraleResponseDTO) {
            EntreeGeneraleResponseDTO dto = (EntreeGeneraleResponseDTO) transaction;
            return dto.getDateCreation() != null ? dto.getDateCreation() : null;
        } else if (transaction instanceof VenteResponse) {
            VenteResponse dto = (VenteResponse) transaction;
            return dto.getDateVente() != null ? dto.getDateVente() : null;
        } else if (transaction instanceof PaiementDTO) {
            PaiementDTO dto = (PaiementDTO) transaction;
            return dto.getDatePaiement() != null ? dto.getDatePaiement() : null;
        } else if (transaction instanceof TransfertFondsResponseDTO) {
            TransfertFondsResponseDTO dto = (TransfertFondsResponseDTO) transaction;
            return dto.getDateTransfert() != null ? dto.getDateTransfert() : null;
        } else if (transaction instanceof FermetureCaisseResponseDTO) {
            FermetureCaisseResponseDTO dto = (FermetureCaisseResponseDTO) transaction;
            return dto.getDateFermeture() != null ? dto.getDateFermeture() : null;
        }
        return null;
    }

    /**
     * Mappe une caisse fermée avec ses ventes vers FermetureCaisseResponseDTO
     */
    private FermetureCaisseResponseDTO mapCaisseFermeeToResponse(Caisse caisse, List<Vente> ventes, Long entrepriseId) {
        FermetureCaisseResponseDTO dto = new FermetureCaisseResponseDTO();
        
        dto.setCaisseId(caisse.getId());
        dto.setDateFermeture(caisse.getDateFermeture());
        dto.setDateOuverture(caisse.getDateOuverture());
        dto.setMontantInitial(caisse.getMontantInitial());
        dto.setMontantCourant(caisse.getMontantCourant());
        dto.setMontantEnMain(caisse.getMontantEnMain());
        dto.setEcart(caisse.getEcart());
        
        // Générer le numéro de fermeture
        String numeroFermeture = genererNumeroFermeture(entrepriseId, caisse.getDateFermeture());
        dto.setNumeroFermeture(numeroFermeture);
        
        // Informations de la boutique
        if (caisse.getBoutique() != null) {
            dto.setBoutiqueId(caisse.getBoutique().getId());
            dto.setNomBoutique(caisse.getBoutique().getNomBoutique());
            dto.setOrigine(caisse.getBoutique().getNomBoutique());
        } else {
            dto.setOrigine("BOUTIQUE");
        }
        
        // Informations du vendeur
        if (caisse.getVendeur() != null) {
            dto.setVendeurId(caisse.getVendeur().getId());
            dto.setNomVendeur(caisse.getVendeur().getNomComplet());
        }
        
        // Calculer le montant total et le nombre de ventes
        Double montantTotal = ventes.stream()
                .mapToDouble(v -> v.getMontantTotal() != null ? v.getMontantTotal() : 0.0)
                .sum();
        dto.setMontantTotal(montantTotal);
        dto.setNombreVentes(ventes.size());
        
        // Mapper les ventes en VenteResponse
        List<VenteResponse> ventesDTO = ventes.stream()
                .map(this::mapVenteToResponse)
                .collect(Collectors.toList());
        dto.setVentes(ventesDTO);
        
        dto.setTypeTransaction("ENTREE");
        
        return dto;
    }

    private VenteResponse mapVenteToResponse(Vente vente) {
        VenteResponse response = new VenteResponse();
        response.setVenteId(vente.getId());
        
        if (vente.getCaisse() != null) {
            VenteResponse.CaisseDTO caisseDTO = new VenteResponse.CaisseDTO();
            caisseDTO.setId(vente.getCaisse().getId());
            caisseDTO.setMontantCourant(vente.getCaisse().getMontantCourant());
            caisseDTO.setStatut(vente.getCaisse().getStatut().name());
            caisseDTO.setDateOuverture(vente.getCaisse().getDateOuverture());
            caisseDTO.setDateFermeture(vente.getCaisse().getDateFermeture());
            response.setCaisse(caisseDTO);
        }
        
        response.setBoutiqueId(vente.getBoutique() != null ? vente.getBoutique().getId() : null);
        response.setVendeurId(vente.getVendeur() != null ? vente.getVendeur().getId() : null);
        response.setDateVente(vente.getDateVente());
        response.setMontantTotal(vente.getMontantTotal());
        response.setDescription(vente.getDescription());
        response.setClientNom(vente.getClientNom());
        response.setClientNumero(vente.getClientNumero());
        response.setModePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : null);
        response.setMontantPaye(vente.getMontantPaye());
        
        List<VenteResponse.LigneVenteDTO> lignesDTO = new ArrayList<>();
        if (vente.getProduits() != null) {
            for (VenteProduit ligne : vente.getProduits()) {
                VenteResponse.LigneVenteDTO dto = new VenteResponse.LigneVenteDTO();
                dto.setProduitId(ligne.getProduit().getId());
                dto.setNomProduit(ligne.getProduit().getNom());
                dto.setQuantite(ligne.getQuantite());
                dto.setPrixUnitaire(ligne.getPrixUnitaire());
                dto.setMontantLigne(ligne.getMontantLigne());
                dto.setRemise(ligne.getRemise());
                dto.setQuantiteRemboursee(ligne.getQuantiteRemboursee());
                dto.setMontantRembourse(ligne.getMontantRembourse());
                dto.setEstRemboursee(ligne.isEstRemboursee());
                lignesDTO.add(dto);
            }
        }
        response.setRemiseGlobale(vente.getRemiseGlobale());
        response.setLignes(lignesDTO);
        
        FactureVente facture = factureVenteRepository.findByVente(vente).orElse(null);
        if (facture != null) {
            response.setNumeroFacture(facture.getNumeroFacture());
        }
        
        response.setNomVendeur(vente.getVendeur() != null ? vente.getVendeur().getNomComplet() : null);
        String nomBoutique = vente.getBoutique() != null ? vente.getBoutique().getNomBoutique() : null;
        response.setNomBoutique(nomBoutique);
        response.setMontantTotalRembourse(vente.getMontantTotalRembourse());
        response.setDateDernierRemboursement(vente.getDateDernierRemboursement());
        response.setNombreRemboursements(vente.getNombreRemboursements());
        response.setTypeTransaction("ENTREE");
        // L'origine pour les ventes est le nom de la boutique
        response.setOrigine(nomBoutique != null ? nomBoutique : "BOUTIQUE");
        
        return response;
    }

    /**
     * Crée une transaction de transfert (SORTIE ou ENTREE) à partir d'un transfert de fonds
     */
    private TransfertFondsResponseDTO creerTransactionTransfert(TransfertFondsResponseDTO transfert, String sens, String origine) {
        TransfertFondsResponseDTO transaction = new TransfertFondsResponseDTO();
        transaction.setId(transfert.getId());
        transaction.setDateTransfert(transfert.getDateTransfert());
        transaction.setMotif(transfert.getMotif());
        transaction.setResponsable(transfert.getResponsable());
        transaction.setDe(transfert.getDe());
        transaction.setVers(transfert.getVers());
        transaction.setMontant(transfert.getMontant());
        transaction.setPersonneALivrer(transfert.getPersonneALivrer());
        transaction.setEntrepriseId(transfert.getEntrepriseId());
        transaction.setEntrepriseNom(transfert.getEntrepriseNom());
        transaction.setTypeTransaction(sens); // SORTIE ou ENTREE
        transaction.setSensTransfert(sens); // SORTIE ou ENTREE
        transaction.setOrigine(origine); // Source ou destination selon le sens
        
        // Créer une description explicite selon le sens
        String description;
        if ("SORTIE".equals(sens)) {
            description = "Transfert sortant de " + origine + " vers " + transfert.getVers() + " - " + transfert.getMotif();
        } else {
            description = "Transfert entrant depuis " + transfert.getDe() + " vers " + origine + " - " + transfert.getMotif();
        }
        transaction.setDescription(description);
        
        return transaction;
    }
}

