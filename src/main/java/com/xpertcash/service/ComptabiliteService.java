package com.xpertcash.service;

import com.xpertcash.DTOs.ComptabiliteDTO;
import com.xpertcash.entity.*;
import com.xpertcash.entity.VENTE.*;
import com.xpertcash.repository.*;
import com.xpertcash.repository.VENTE.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        comptabilite.setBoutiques(calculerBoutiques(entrepriseId));
        comptabilite.setBoutiquesDisponibles(listerBoutiquesDisponibles(entrepriseId));

        comptabilite.setClients(calculerClients(entrepriseId));

        comptabilite.setVendeurs(calculerVendeurs(entrepriseId));

        comptabilite.setActivites(calculerActivites(entrepriseId));

        return comptabilite;
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

            boutiquesInfo.add(new ComptabiliteDTO.BoutiqueInfoDTO(
                    boutique.getId(),
                    boutique.getNomBoutique(),
                    chiffreAffaires,
                    ventes.size(),
                    totalDepenses,
                    depenses.size()
            ));
        }

        return boutiquesInfo;
    }

    /**
     * Liste toutes les boutiques de l'entreprise avec leurs informations de base
     */
    private List<ComptabiliteDTO.BoutiqueDisponibleDTO> listerBoutiquesDisponibles(Long entrepriseId) {
        List<Boutique> boutiques = boutiqueRepository.findByEntrepriseId(entrepriseId);
        return boutiques.stream().map(b -> {
            ComptabiliteDTO.BoutiqueDisponibleDTO d = new ComptabiliteDTO.BoutiqueDisponibleDTO();
            d.setId(b.getId());
            d.setNom(b.getNomBoutique());
            d.setType(b.getTypeBoutique() != null ? b.getTypeBoutique().name() : null);
            d.setEmail(b.getEmail());
            d.setAdresse(b.getAdresse());
            d.setTelephone(b.getTelephone());
            d.setDateCreation(b.getCreatedAt());
            d.setStatut(b.isActif() ? "Actif" : "Inactif");
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

        int nombreTransactionsJour = ventesJour.size() + facturesJour.size() + depensesJour.size();

        return new ComptabiliteDTO.ActivitesDTO(
                toutesVentes.size(),
                toutesFactures.size(),
                toutesDepenses.size(),
                nombreTransactionsJour
        );
    }
}

