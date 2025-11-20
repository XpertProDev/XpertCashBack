package com.xpertcash.service;

import com.xpertcash.DTOs.TresorerieDTO;
import com.xpertcash.entity.*;
import com.xpertcash.entity.Enum.SourceDepense;
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
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TresorerieService {

    private static final Logger logger = LoggerFactory.getLogger(TresorerieService.class);

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private CaisseRepository caisseRepository;

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private VenteRepository venteRepository;

    @Autowired
    private PaiementRepository paiementRepository;

    @Autowired
    private FactureReelleRepository factureReelleRepository;

    @Autowired
    private DepenseGeneraleRepository depenseGeneraleRepository;

    @Autowired
    private EntreeGeneraleRepository entreeGeneraleRepository;

    @Autowired
    private MouvementCaisseRepository mouvementCaisseRepository;

    @Autowired
    private VenteHistoriqueRepository venteHistoriqueRepository;

    @Transactional(readOnly = true)
    public TresorerieDTO calculerTresorerie(HttpServletRequest request) {
        Long entrepriseId = validerEntreprise(request);
        return calculerTresorerieParEntrepriseId(entrepriseId);
    }

    @Transactional(readOnly = true)
    public TresorerieDTO calculerTresorerieParEntrepriseId(Long entrepriseId) {
        try {
            TresorerieData data = chargerDonnees(entrepriseId);
            TresorerieDTO tresorerie = new TresorerieDTO();

            TresorerieDTO.CaisseDetail caisseDetail = calculerCaisse(data);
            tresorerie.setCaisseDetail(caisseDetail);
            tresorerie.setMontantCaisse(caisseDetail.getMontantTotal());

            TresorerieDTO.BanqueDetail banqueDetail = calculerBanque(data);
            tresorerie.setBanqueDetail(banqueDetail);
            tresorerie.setMontantBanque(banqueDetail.getSolde());

            TresorerieDTO.MobileMoneyDetail mobileMoneyDetail = calculerMobileMoney(data);
            tresorerie.setMobileMoneyDetail(mobileMoneyDetail);
            tresorerie.setMontantMobileMoney(mobileMoneyDetail.getSolde());

            TresorerieDTO.DetteDetail detteDetail = calculerDette(data);
            tresorerie.setDetteDetail(detteDetail);
            tresorerie.setMontantDette(detteDetail.getTotal());

            tresorerie.setTotalTresorerie(
                tresorerie.getMontantCaisse() + 
                tresorerie.getMontantBanque() + 
                tresorerie.getMontantMobileMoney()
            );

            return tresorerie;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Erreur lors du calcul de la trésorerie pour l'entreprise {}", entrepriseId, e);
            throw new BusinessException("Erreur lors du calcul de la trésorerie : " + e.getMessage());
        }
    }

    private Long validerEntreprise(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        if (user.getEntreprise() == null) {
            throw new BusinessException("Vous n'êtes associé à aucune entreprise.");
        }
        return user.getEntreprise().getId();
    }

    private static class TresorerieData {
        final List<Long> boutiqueIds;
        final List<Caisse> caissesFermees;
        final List<Vente> toutesVentes;
        final Map<Long, Double> remboursementsParVente;
        final List<FactureReelle> factures;
        final Map<Long, BigDecimal> paiementsParFacture;
        final List<DepenseGenerale> depensesGenerales;
        final List<EntreeGenerale> entreesGenerales;
        final List<MouvementCaisse> mouvementsDepense;
        final List<MouvementCaisse> mouvementsRetrait;

        TresorerieData(List<Long> boutiqueIds, List<Caisse> caissesFermees,
                      List<Vente> toutesVentes, Map<Long, Double> remboursementsParVente,
                      List<FactureReelle> factures, Map<Long, BigDecimal> paiementsParFacture,
                      List<DepenseGenerale> depensesGenerales, List<EntreeGenerale> entreesGenerales,
                      List<MouvementCaisse> mouvementsDepense, List<MouvementCaisse> mouvementsRetrait) {
            this.boutiqueIds = boutiqueIds;
            this.caissesFermees = caissesFermees;
            this.toutesVentes = toutesVentes;
            this.remboursementsParVente = remboursementsParVente;
            this.factures = factures;
            this.paiementsParFacture = paiementsParFacture;
            this.depensesGenerales = depensesGenerales;
            this.entreesGenerales = entreesGenerales;
            this.mouvementsDepense = mouvementsDepense;
            this.mouvementsRetrait = mouvementsRetrait;
        }
    }

    private TresorerieData chargerDonnees(Long entrepriseId) {
        List<Boutique> boutiques = boutiqueRepository.findByEntrepriseId(entrepriseId);
        List<Long> boutiqueIds = boutiques.stream()
                .map(Boutique::getId)
                .collect(Collectors.toList());

        List<Caisse> caissesFermees = chargerCaissesFermees(entrepriseId);
        List<Vente> toutesVentes = venteRepository.findAllByEntrepriseId(entrepriseId);
        Map<Long, Double> remboursementsParVente = calculerRemboursementsParVente(toutesVentes);
        List<FactureReelle> factures = factureReelleRepository.findByEntrepriseId(entrepriseId);
        Map<Long, BigDecimal> paiementsParFacture = chargerPaiementsParFacture(factures);
        List<DepenseGenerale> depensesGenerales = depenseGeneraleRepository.findByEntrepriseId(entrepriseId);
        List<EntreeGenerale> entreesGenerales = entreeGeneraleRepository.findByEntrepriseId(entrepriseId);
        List<MouvementCaisse> mouvementsDepense = mouvementCaisseRepository
                .findByCaisse_Boutique_Entreprise_IdAndTypeMouvement(entrepriseId, TypeMouvementCaisse.DEPENSE);
        List<MouvementCaisse> mouvementsRetrait = mouvementCaisseRepository
                .findByCaisse_Boutique_Entreprise_IdAndTypeMouvement(entrepriseId, TypeMouvementCaisse.RETRAIT);

        return new TresorerieData(boutiqueIds, caissesFermees, toutesVentes,
                remboursementsParVente, factures, paiementsParFacture, depensesGenerales, entreesGenerales,
                mouvementsDepense, mouvementsRetrait);
    }

    private List<Caisse> chargerCaissesFermees(Long entrepriseId) {
        return caisseRepository.findByEntrepriseIdAndStatut(entrepriseId, StatutCaisse.FERMEE);
    }

    private Map<Long, Double> calculerRemboursementsParVente(List<Vente> ventes) {
        List<Long> venteIds = ventes.stream()
                .map(Vente::getId)
                .collect(Collectors.toList());

        if (venteIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> remboursements = venteHistoriqueRepository.sumRemboursementsByVenteIds(venteIds);
        return remboursements.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).doubleValue(),
                        (v1, v2) -> v1
                ));
    }

    private Map<Long, BigDecimal> chargerPaiementsParFacture(List<FactureReelle> factures) {
        List<Long> factureIds = factures.stream()
                .map(FactureReelle::getId)
                .collect(Collectors.toList());

        if (factureIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> paiements = paiementRepository.sumMontantsByFactureReelleIds(factureIds);
        return paiements.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]
                ));
    }

    private TresorerieDTO.CaisseDetail calculerCaisse(TresorerieData data) {
        if (data.boutiqueIds.isEmpty()) {
            return creerCaisseDetailVide();
        }

        double montantTotalCaisse = calculerMontantTotalCaisses(data.caissesFermees);
        double entreesVentes = calculerEntreesVentes(data.toutesVentes, data.remboursementsParVente,
                ModePaiement.ESPECES);
        double entreesGeneralesCaisse = calculerEntreesGeneralesCaisse(data);
        double entrees = entreesVentes + entreesGeneralesCaisse;
        double sorties = calculerSortiesCaisse(data);

        TresorerieDTO.CaisseDetail detail = new TresorerieDTO.CaisseDetail();
        detail.setNombreCaissesOuvertes(data.caissesFermees.size());
        detail.setMontantTotal(montantTotalCaisse);
        detail.setEntrees(entrees);
        detail.setSorties(sorties);
        return detail;
    }

    private double calculerMontantTotalCaisses(List<Caisse> caisses) {
        return caisses.stream()
                .mapToDouble(c -> getValeurDouble(c.getMontantCourant()))
                .sum();
    }

    private double calculerEntreesVentes(List<Vente> ventes, Map<Long, Double> remboursementsParVente,
                                          ModePaiement modePaiement) {
        double totalVentes = ventes.stream()
                .filter(v -> v.getModePaiement() == modePaiement
                        && v.getStatus() != VenteStatus.REMBOURSEE)
                .mapToDouble(v -> getValeurDouble(v.getMontantTotal()))
                .sum();

        double totalRemboursements = remboursementsParVente.entrySet().stream()
                .filter(e -> ventes.stream()
                        .anyMatch(v -> v.getId().equals(e.getKey())
                                && v.getModePaiement() == modePaiement))
                .mapToDouble(Map.Entry::getValue)
                .sum();

        return totalVentes - totalRemboursements;
    }

    private double calculerSortiesCaisse(TresorerieData data) {
        double depensesEspeces = data.mouvementsDepense.stream()
                .filter(m -> m.getModePaiement() == ModePaiement.ESPECES)
                .mapToDouble(m -> getValeurDouble(m.getMontant()))
                .sum();

        double retraits = data.mouvementsRetrait.stream()
                .mapToDouble(m -> getValeurDouble(m.getMontant()))
                .sum();

        double depensesGeneralesCaisse = data.depensesGenerales.stream()
                .filter(d -> d.getSource() == SourceDepense.CAISSE)
                .mapToDouble(d -> getValeurDouble(d.getMontant()))
                .sum();

        return depensesEspeces + retraits + depensesGeneralesCaisse;
    }

    private TresorerieDTO.BanqueDetail calculerBanque(TresorerieData data) {
        return calculerDetailBanqueOuMobileMoney(data, SourceDepense.BANQUE,
                ModePaiement.VIREMENT, ModePaiement.CHEQUE);
    }

    private TresorerieDTO.MobileMoneyDetail calculerMobileMoney(TresorerieData data) {
        TresorerieDTO.BanqueDetail detail = calculerDetailBanqueOuMobileMoney(data,
                SourceDepense.MOBILE_MONEY, ModePaiement.MOBILE_MONEY, null);

        TresorerieDTO.MobileMoneyDetail mobileMoneyDetail = new TresorerieDTO.MobileMoneyDetail();
        mobileMoneyDetail.setEntrees(detail.getEntrees());
        mobileMoneyDetail.setSorties(detail.getSorties());
        mobileMoneyDetail.setSolde(detail.getSolde());
        return mobileMoneyDetail;
    }

    private TresorerieDTO.BanqueDetail calculerDetailBanqueOuMobileMoney(TresorerieData data,
                                                                          SourceDepense sourceDepense,
                                                                          ModePaiement modePaiement1,
                                                                          ModePaiement modePaiement2) {
        double entreesVentes = calculerEntreesVentesBanqueOuMobileMoney(data, modePaiement1, modePaiement2);
        double entreesPaiements = calculerEntreesPaiementsFactures(data, modePaiement1, modePaiement2);
        double entreesGenerales = calculerEntreesGeneralesParSource(data, sourceDepense);
        double entrees = entreesVentes + entreesPaiements + entreesGenerales;

        double sortiesDepenses = data.depensesGenerales.stream()
                .filter(d -> d.getSource() == sourceDepense)
                .mapToDouble(d -> getValeurDouble(d.getMontant()))
                .sum();

        double sortiesMouvements = data.mouvementsDepense.stream()
                .filter(m -> correspondModePaiement(m.getModePaiement(), modePaiement1, modePaiement2))
                .mapToDouble(m -> getValeurDouble(m.getMontant()))
                .sum();

        double sorties = sortiesDepenses + sortiesMouvements;

        TresorerieDTO.BanqueDetail detail = new TresorerieDTO.BanqueDetail();
        detail.setEntrees(entrees);
        detail.setSorties(sorties);
        detail.setSolde(entrees - sorties);
        return detail;
    }

    private double calculerEntreesVentesBanqueOuMobileMoney(TresorerieData data,
                                                             ModePaiement modePaiement1,
                                                             ModePaiement modePaiement2) {
        double totalVentes = data.toutesVentes.stream()
                .filter(v -> correspondModePaiement(v.getModePaiement(), modePaiement1, modePaiement2)
                        && v.getStatus() != VenteStatus.REMBOURSEE)
                .mapToDouble(v -> getValeurDouble(v.getMontantTotal()))
                .sum();

        double totalRemboursements = data.remboursementsParVente.entrySet().stream()
                .filter(e -> data.toutesVentes.stream()
                        .anyMatch(v -> v.getId().equals(e.getKey())
                                && correspondModePaiement(v.getModePaiement(), modePaiement1, modePaiement2)))
                .mapToDouble(Map.Entry::getValue)
                .sum();

        return totalVentes - totalRemboursements;
    }

    private double calculerEntreesPaiementsFactures(TresorerieData data,
                                                    ModePaiement modePaiement1,
                                                    ModePaiement modePaiement2) {
        if (data.factures.isEmpty()) {
            return 0.0;
        }

        List<Long> factureIds = data.factures.stream()
                .map(FactureReelle::getId)
                .collect(Collectors.toList());

        List<Paiement> paiements = paiementRepository.findByFactureReelle_IdIn(factureIds);
        return paiements.stream()
                .filter(p -> correspondModePaiementString(p.getModePaiement(), modePaiement1, modePaiement2))
                .mapToDouble(p -> getValeurDouble(p.getMontant()))
                .sum();
    }

    private boolean correspondModePaiement(ModePaiement modePaiement, ModePaiement mode1, ModePaiement mode2) {
        if (modePaiement == null) {
            return false;
        }
        return modePaiement == mode1 || (mode2 != null && modePaiement == mode2);
    }

    private boolean correspondModePaiementString(String modePaiement, ModePaiement mode1, ModePaiement mode2) {
        if (modePaiement == null) {
            return false;
        }
        String mode1Str = mode1.name();
        String mode2Str = mode2 != null ? mode2.name() : null;
        return modePaiement.equals(mode1Str) || (mode2Str != null && modePaiement.equals(mode2Str));
    }

    private TresorerieDTO.DetteDetail calculerDette(TresorerieData data) {
        double facturesImpayees = 0.0;
        int nombreFacturesImpayees = 0;

        for (FactureReelle facture : data.factures) {
            BigDecimal totalPaye = data.paiementsParFacture.getOrDefault(facture.getId(), BigDecimal.ZERO);
            double montantRestant = facture.getTotalFacture() - totalPaye.doubleValue();

            if (montantRestant > 0) {
                facturesImpayees += montantRestant;
                nombreFacturesImpayees++;
            }
        }

        List<DepenseGenerale> depensesDette = data.depensesGenerales.stream()
                .filter(d -> d.getSource() == SourceDepense.DETTE)
                .collect(Collectors.toList());

        double montantDepensesDette = depensesDette.stream()
                .mapToDouble(d -> getValeurDouble(d.getMontant()))
                .sum();

        TresorerieDTO.DetteDetail detail = new TresorerieDTO.DetteDetail();
        detail.setFacturesImpayees(facturesImpayees);
        detail.setNombreFacturesImpayees(nombreFacturesImpayees);
        detail.setDepensesDette(montantDepensesDette);
        detail.setNombreDepensesDette(depensesDette.size());
        detail.setTotal(facturesImpayees + montantDepensesDette);
        return detail;
    }

    private TresorerieDTO.CaisseDetail creerCaisseDetailVide() {
        TresorerieDTO.CaisseDetail detail = new TresorerieDTO.CaisseDetail();
        detail.setNombreCaissesOuvertes(0);
        detail.setMontantTotal(0.0);
        detail.setEntrees(0.0);
        detail.setSorties(0.0);
        return detail;
    }

    private double calculerEntreesGeneralesCaisse(TresorerieData data) {
        return data.entreesGenerales.stream()
                .filter(e -> e.getSource() == SourceDepense.CAISSE)
                .mapToDouble(e -> getValeurDouble(e.getMontant()))
                .sum();
    }

    private double calculerEntreesGeneralesParSource(TresorerieData data, SourceDepense sourceDepense) {
        return data.entreesGenerales.stream()
                .filter(e -> e.getSource() == sourceDepense)
                .mapToDouble(e -> getValeurDouble(e.getMontant()))
                .sum();
    }

    private double getValeurDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private double getValeurDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }
}
