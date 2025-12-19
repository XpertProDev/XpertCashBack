package com.xpertcash.service;

import com.xpertcash.DTOs.DetteItemDTO;
import com.xpertcash.DTOs.PaginatedResponseDTO;
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

    @Autowired
    private FactureVenteRepository factureVenteRepository;

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
            
            double depensesGeneralesCaisse = data.depensesGenerales.stream()
                    .filter(d -> d.getSource() == SourceDepense.CAISSE)
                    .mapToDouble(d -> getValeurDouble(d.getMontant()))
                    .sum();
            
            double entreesGeneralesCaisse = calculerEntreesGeneralesCaisse(data);
            double entreesPaiementsEspeces = calculerEntreesPaiementsFactures(data, ModePaiement.ESPECES, null);
            
            // montantCaisse = montantTotal (des caisses fermées) + entrées générales + paiements en espèces - dépenses générales
            double montantCaisseReel = caisseDetail.getMontantTotal() + entreesGeneralesCaisse + entreesPaiementsEspeces - depensesGeneralesCaisse;
            tresorerie.setMontantCaisse(Math.max(0.0, montantCaisseReel));

            TresorerieDTO.BanqueDetail banqueDetail = calculerBanque(data);
            tresorerie.setBanqueDetail(banqueDetail);
            tresorerie.setMontantBanque(banqueDetail.getSolde());

            TresorerieDTO.MobileMoneyDetail mobileMoneyDetail = calculerMobileMoney(data);
            tresorerie.setMobileMoneyDetail(mobileMoneyDetail);
            tresorerie.setMontantMobileMoney(mobileMoneyDetail.getSolde());

            TresorerieDTO.DetteDetail detteDetail = calculerDette(data, entrepriseId);
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

    /**
     * Récupère la liste paginée des dettes (factures impayées, ventes à crédit, dépenses en DETTE)
     * pour l'entreprise de l'utilisateur connecté.
     */
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<DetteItemDTO> getDettesDetaillees(HttpServletRequest request, int page, int size) {
        Long entrepriseId = validerEntreprise(request);
        TresorerieData data = chargerDonnees(entrepriseId);

        // Construire la liste complète des dettes
        java.util.List<DetteItemDTO> items = new java.util.ArrayList<>();

        // 1️⃣ Factures réelles impayées
        for (FactureReelle facture : data.factures) {
            BigDecimal totalPaye = data.paiementsParFacture.getOrDefault(facture.getId(), BigDecimal.ZERO);
            double montantRestant = facture.getTotalFacture() - totalPaye.doubleValue();

            if (montantRestant > 0) {
                DetteItemDTO dto = new DetteItemDTO();
                dto.setId(facture.getId());
                dto.setType("FACTURE_IMPAYEE");
                // dto.setSource("FACTURE");
                dto.setMontantRestant(montantRestant);
                dto.setDate(facture.getDateCreationPro() != null
                        ? facture.getDateCreationPro()
                        : (facture.getDateCreation() != null ? facture.getDateCreation().atStartOfDay() : null));
                dto.setDescription(facture.getDescription());
                dto.setNumero(facture.getNumeroFacture());

                if (facture.getClient() != null) {
                    dto.setClient(facture.getClient().getNomComplet());
                    dto.setContact(facture.getClient().getTelephone());
                } else if (facture.getEntrepriseClient() != null) {
                    dto.setClient(facture.getEntrepriseClient().getNom());
                    dto.setContact(facture.getEntrepriseClient().getTelephone());
                }

                items.add(dto);
            }
        }

        // 2️⃣ Dépenses générales avec source DETTE
        java.util.List<DepenseGenerale> depensesDette = data.depensesGenerales.stream()
                .filter(d -> d.getSource() == SourceDepense.DETTE)
                .collect(Collectors.toList());

        for (DepenseGenerale depense : depensesDette) {
            DetteItemDTO dto = new DetteItemDTO();
            dto.setId(depense.getId());
            dto.setType("DEPENSE_DETTE");
            // dto.setSource("DEPENSE");
            dto.setMontantRestant(getValeurDouble(depense.getMontant()));
            dto.setDate(depense.getDateCreation());
            dto.setDescription(depense.getDesignation());
            dto.setNumero(depense.getNumero());

            if (depense.getFournisseur() != null) {
                dto.setClient(depense.getFournisseur().getNomComplet());
                dto.setContact(depense.getFournisseur().getTelephone());
            }

            items.add(dto);
        }

        // 3️⃣ Entrées générales avec source DETTE (dettes à encaisser)
        java.util.List<EntreeGenerale> entreesDette = data.entreesGenerales.stream()
                .filter(e -> e.getSource() == SourceDepense.DETTE)
                .collect(Collectors.toList());

        for (EntreeGenerale entree : entreesDette) {
            DetteItemDTO dto = new DetteItemDTO();
            dto.setId(entree.getId());
            dto.setType("ENTREE_DETTE");
            dto.setMontantRestant(getValeurDouble(entree.getMontant()));
            dto.setDate(entree.getDateCreation());
            dto.setDescription(entree.getDesignation());
            dto.setNumero(entree.getNumero());

            if (entree.getResponsable() != null) {
                dto.setClient(entree.getResponsable().getNomComplet());
                dto.setContact(entree.getResponsable().getPhone());
            }

            items.add(dto);
        }

        // 4️⃣ Ventes à crédit (CREDIT)
        java.util.List<Vente> ventesCredit = venteRepository.findByBoutique_Entreprise_IdAndModePaiement(entrepriseId, ModePaiement.CREDIT);
        for (Vente v : ventesCredit) {
            double total = getValeurDouble(v.getMontantTotal());
            double rembourse = getValeurDouble(v.getMontantTotalRembourse());
            double restant = total - rembourse;
            if (restant <= 0) {
                continue;
            }

            DetteItemDTO dto = new DetteItemDTO();
            dto.setId(v.getId());
            dto.setType("VENTE_CREDIT");
            // dto.setSource("POS");
            dto.setMontantRestant(restant);
            dto.setDate(v.getDateVente());
            dto.setDescription(v.getDescription());
            // Numéro de facture vente si disponible
            factureVenteRepository.findByVenteId(v.getId())
                    .ifPresent(f -> dto.setNumero(f.getNumeroFacture()));
            // Client pouvant être un Client ou une EntrepriseClient, ou juste un nom/numéro libre
            if (v.getClient() != null) {
                dto.setClient(v.getClient().getNomComplet());
                dto.setContact(v.getClient().getTelephone());
            } else if (v.getEntrepriseClient() != null) {
                dto.setClient(v.getEntrepriseClient().getNom());
                dto.setContact(v.getEntrepriseClient().getTelephone());
            } else {
                dto.setClient(v.getClientNom());
                dto.setContact(v.getClientNumero());
            }

            items.add(dto);
        }

        // Tri par date décroissante (nulls en dernier)
        items.sort((a, b) -> {
            if (a.getDate() == null && b.getDate() == null) return 0;
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

        // Pagination manuelle
        int totalElements = items.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        java.util.List<DetteItemDTO> pageContent =
                (fromIndex >= totalElements) ? java.util.Collections.emptyList() : items.subList(fromIndex, toIndex);

        PaginatedResponseDTO<DetteItemDTO> response = new PaginatedResponseDTO<>();
        response.setContent(pageContent);
        response.setPageNumber(page);
        response.setPageSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);
        response.setHasNext(page < totalPages - 1);
        response.setHasPrevious(page > 0);
        response.setFirst(page == 0);
        response.setLast(page >= totalPages - 1 || totalPages == 0);

        return response;
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
        List<Long> caisseIdsFermees = caissesFermees.stream().map(Caisse::getId).collect(Collectors.toList());
        List<Vente> toutesVentes = venteRepository.findAllByEntrepriseId(entrepriseId);
        List<Vente> ventesCaissesFermees = toutesVentes.stream()
                .filter(v -> v.getCaisse() != null && caisseIdsFermees.contains(v.getCaisse().getId()))
                .collect(Collectors.toList());
        Map<Long, Double> remboursementsParVente = calculerRemboursementsParVente(ventesCaissesFermees);
        List<FactureReelle> factures = factureReelleRepository.findByEntrepriseId(entrepriseId);
        Map<Long, BigDecimal> paiementsParFacture = chargerPaiementsParFacture(factures);
        List<DepenseGenerale> depensesGenerales = depenseGeneraleRepository.findByEntrepriseId(entrepriseId);
        List<EntreeGenerale> entreesGenerales = entreeGeneraleRepository.findByEntrepriseId(entrepriseId);
        
        List<MouvementCaisse> tousMouvementsDepense = mouvementCaisseRepository
                .findByCaisse_Boutique_Entreprise_IdAndTypeMouvement(entrepriseId, TypeMouvementCaisse.DEPENSE);
        List<MouvementCaisse> mouvementsDepense = tousMouvementsDepense.stream()
                .filter(m -> m.getCaisse() != null && caisseIdsFermees.contains(m.getCaisse().getId()))
                .collect(Collectors.toList());
        
        List<MouvementCaisse> tousMouvementsRetrait = mouvementCaisseRepository
                .findByCaisse_Boutique_Entreprise_IdAndTypeMouvement(entrepriseId, TypeMouvementCaisse.RETRAIT);
        List<MouvementCaisse> mouvementsRetrait = tousMouvementsRetrait.stream()
                .filter(m -> m.getCaisse() != null && caisseIdsFermees.contains(m.getCaisse().getId()))
                .collect(Collectors.toList());

        return new TresorerieData(boutiqueIds, caissesFermees, ventesCaissesFermees,
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
        
        List<Long> caisseIdsFermees = data.caissesFermees.stream().map(Caisse::getId).collect(Collectors.toList());
        
        double entreesMouvementsVente = 0.0;
        double entreesMouvementsAjout = 0.0;
        if (!caisseIdsFermees.isEmpty()) {
            List<MouvementCaisse> mouvementsVente = mouvementCaisseRepository.findByCaisseIdInAndTypeMouvement(
                caisseIdsFermees, TypeMouvementCaisse.VENTE);
            entreesMouvementsVente = mouvementsVente.stream()
                    .filter(m -> m.getModePaiement() == ModePaiement.ESPECES)
                    .mapToDouble(m -> getValeurDouble(m.getMontant()))
                    .sum();
            
            List<MouvementCaisse> mouvementsAjout = mouvementCaisseRepository.findByCaisseIdInAndTypeMouvement(
                caisseIdsFermees, TypeMouvementCaisse.AJOUT);
            entreesMouvementsAjout = mouvementsAjout.stream()
                    .filter(m -> m.getModePaiement() == ModePaiement.ESPECES)
                    .mapToDouble(m -> getValeurDouble(m.getMontant()))
                    .sum();
        }
        
        double entreesPaiementsEspeces = calculerEntreesPaiementsFactures(data, ModePaiement.ESPECES, null);
        double entreesGeneralesCaisse = calculerEntreesGeneralesCaisse(data);
        double entrees = entreesMouvementsVente + entreesMouvementsAjout + entreesPaiementsEspeces + entreesGeneralesCaisse;
        
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
        double entreesVentesVirement = calculerEntreesVentesBanqueOuMobileMoney(data, ModePaiement.VIREMENT, ModePaiement.CHEQUE);
        double entreesVentesCarte = calculerEntreesVentesBanqueOuMobileMoney(data, ModePaiement.CARTE, null);
        double entreesVentes = entreesVentesVirement + entreesVentesCarte;
        
        double entreesPaiementsVirement = calculerEntreesPaiementsFactures(data, ModePaiement.VIREMENT, ModePaiement.CHEQUE);
        double entreesPaiementsCarte = calculerEntreesPaiementsFactures(data, ModePaiement.CARTE, null);
        double entreesPaiements = entreesPaiementsVirement + entreesPaiementsCarte;
        
        double entreesGenerales = calculerEntreesGeneralesParSource(data, SourceDepense.BANQUE);
        double entrees = entreesVentes + entreesPaiements + entreesGenerales;

        double sortiesDepenses = data.depensesGenerales.stream()
                .filter(d -> d.getSource() == SourceDepense.BANQUE)
                .mapToDouble(d -> getValeurDouble(d.getMontant()))
                .sum();

        double sortiesMouvements = data.mouvementsDepense.stream()
                .filter(m -> correspondModePaiement(m.getModePaiement(), ModePaiement.VIREMENT, ModePaiement.CHEQUE)
                        || correspondModePaiement(m.getModePaiement(), ModePaiement.CARTE, null))
                .mapToDouble(m -> getValeurDouble(m.getMontant()))
                .sum();

        double sorties = sortiesDepenses + sortiesMouvements;

        TresorerieDTO.BanqueDetail detail = new TresorerieDTO.BanqueDetail();
        detail.setEntrees(entrees);
        detail.setSorties(sorties);
        detail.setSolde(entrees - sorties);
        return detail;
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

        if (factureIds.isEmpty()) {
            return 0.0;
        }

        List<Paiement> paiements = paiementRepository.findByFactureReelle_IdIn(factureIds);
        
        logger.info("Calcul paiements factures - Factures: {}, Mode1: {}, Mode2: {}, Total paiements récupérés: {}", 
                factureIds.size(), modePaiement1, modePaiement2, paiements.size());
        
        for (Paiement p : paiements) {
            logger.info("Paiement trouvé - ID: {}, Mode: '{}', Montant: {}, FactureID: {}", 
                    p.getId(), p.getModePaiement(), p.getMontant(), 
                    p.getFactureReelle() != null ? p.getFactureReelle().getId() : null);
        }
        
        double total = paiements.stream()
                .filter(p -> {
                    String modeNormalise = p.getModePaiement() != null ? normaliserModePaiement(p.getModePaiement()) : null;
                    boolean correspond = correspondModePaiementString(p.getModePaiement(), modePaiement1, modePaiement2);
                    logger.info("Paiement ID {} - Mode original: '{}', Mode normalisé: '{}', Correspond à {} ou {}: {}", 
                            p.getId(), p.getModePaiement(), modeNormalise, modePaiement1, modePaiement2, correspond);
                    return correspond;
                })
                .mapToDouble(p -> getValeurDouble(p.getMontant()))
                .sum();
        
        logger.info("Total paiements factures calculé - Mode1: {}, Mode2: {}, Total montant: {}", 
                modePaiement1, modePaiement2, total);
        
        return total;
    }

    private boolean correspondModePaiement(ModePaiement modePaiement, ModePaiement mode1, ModePaiement mode2) {
        if (modePaiement == null) {
            return false;
        }
        return modePaiement == mode1 || (mode2 != null && modePaiement == mode2);
    }

    private boolean correspondModePaiementString(String modePaiement, ModePaiement mode1, ModePaiement mode2) {
        if (modePaiement == null || modePaiement.trim().isEmpty()) {
            return false;
        }
        String modePaiementNormalise = normaliserModePaiement(modePaiement.trim().toUpperCase());
        String mode1Str = mode1.name();
        String mode2Str = mode2 != null ? mode2.name() : null;
        return modePaiementNormalise.equals(mode1Str) || (mode2Str != null && modePaiementNormalise.equals(mode2Str));
    }

    private String normaliserModePaiement(String modePaiement) {
        if (modePaiement == null) {
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
            default:
                return normalise;
        }
    }

    private TresorerieDTO.DetteDetail calculerDette(TresorerieData data, Long entrepriseId) {
        double facturesImpayees = 0.0;
        int nombreFacturesImpayees = 0;

        logger.info("Calcul de la dette pour l'entreprise {}", entrepriseId);

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

        // 💰 Dettes issues des entrées générales marquées comme DETTE (créances à encaisser)
        List<EntreeGenerale> entreesDette = data.entreesGenerales.stream()
                .filter(e -> e.getSource() == SourceDepense.DETTE)
                .collect(Collectors.toList());

        double montantEntreesDette = entreesDette.stream()
                .mapToDouble(e -> getValeurDouble(e.getMontant()))
                .sum();

        // 💳 Dettes issues des ventes à crédit (CREDIT) pour cette entreprise
        List<Vente> ventesCredit = venteRepository.findByBoutique_Entreprise_IdAndModePaiement(entrepriseId, ModePaiement.CREDIT);
        logger.info("Ventes à crédit trouvées pour l'entreprise {} : {}", entrepriseId, ventesCredit.size());

        double montantVentesCredit = 0.0;
        int nombreVentesCredit = 0;

        for (Vente v : ventesCredit) {
            double total = getValeurDouble(v.getMontantTotal());
            double rembourse = getValeurDouble(v.getMontantTotalRembourse());
            double restant = total - rembourse;
            logger.info("Vente CREDIT id={}, total={}, rembourse={}, restant={}", v.getId(), total, rembourse, restant);
            if (restant > 0) {
                montantVentesCredit += restant;
                nombreVentesCredit++;
            }
        }

        logger.info("Total ventes à crédit restantes: {}, nombreVentesCredit: {}", montantVentesCredit, nombreVentesCredit);

        double totalFacturesEtCredits = facturesImpayees + montantVentesCredit;
        int totalNombreFacturesEtCredits = nombreFacturesImpayees + nombreVentesCredit;

        TresorerieDTO.DetteDetail detail = new TresorerieDTO.DetteDetail();
        detail.setFacturesImpayees(totalFacturesEtCredits);
        detail.setNombreFacturesImpayees(totalNombreFacturesEtCredits);
        // On agrège ici les "dettes" provenant des dépenses en DETTE et des entrées en DETTE
        detail.setDepensesDette(montantDepensesDette + montantEntreesDette);
        detail.setNombreDepensesDette(depensesDette.size() + entreesDette.size());
        detail.setTotal(totalFacturesEtCredits + montantDepensesDette + montantEntreesDette);
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
