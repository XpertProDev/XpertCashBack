package com.xpertcash.service;

import com.xpertcash.DTOs.ComptabiliteDTO;
import com.xpertcash.DTOs.CategorieDepenseDTO;
import com.xpertcash.DTOs.DepenseGeneraleRequestDTO;
import com.xpertcash.DTOs.DepenseGeneraleResponseDTO;
import com.xpertcash.entity.*;
import com.xpertcash.entity.Enum.*;
import com.xpertcash.entity.VENTE.*;
import com.xpertcash.repository.*;
import com.xpertcash.repository.VENTE.*;
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
    private ProduitRepository produitRepository;

    @Autowired
    private FournisseurRepository fournisseurRepository;

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
}

