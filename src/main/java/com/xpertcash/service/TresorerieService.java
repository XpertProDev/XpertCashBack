package com.xpertcash.service;

import com.xpertcash.DTOs.DetteItemDTO;
import com.xpertcash.DTOs.PaginatedResponseDTO;
import com.xpertcash.DTOs.TresorerieDTO;
import com.xpertcash.DTOs.TresorerieCaissePeriodeDTO;
import com.xpertcash.DTOs.TresorerieBanquePeriodeDTO;
import com.xpertcash.DTOs.TresorerieMobilePeriodeDTO;
import com.xpertcash.configuration.CentralAccess;
import com.xpertcash.entity.*;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.SourceDepense;
import com.xpertcash.entity.Enum.SourceTresorerie;
import com.xpertcash.entity.PermissionType;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Autowired
    private TresorerieDettesDetailleesRepository tresorerieDettesDetailleesRepository;

    @Autowired
    private TransfertFondsRepository transfertFondsRepository;

     // Calcule la trésorerie complète de l'entreprise de l'utilisateur connecté.
    @Transactional(readOnly = true)
    public TresorerieDTO calculerTresorerie(HttpServletRequest request) {
        Long entrepriseId = validerEntrepriseEtPermissions(request);
        return calculerTresorerieParEntrepriseId(entrepriseId);
    }

    /*
        Calcule la trésorerie avec filtrage par période.
        periode Type de période : "aujourdhui", "hier", "semaine", "mois", "annee", "personnalise"
     */
    @Transactional(readOnly = true)
    public TresorerieDTO calculerTresorerie(HttpServletRequest request, String periode, LocalDate dateDebut, LocalDate dateFin) {
        Long entrepriseId = validerEntrepriseEtPermissions(request);
        
        // Calculer les dates selon la période
        PeriodeDates periodeDates = calculerDatesPeriode(periode, dateDebut, dateFin);
        
        return calculerTresorerieParEntrepriseIdAvecPeriode(entrepriseId, periodeDates);
    }

    /**
     * Détail de la caisse pour une période donnée (utilisé pour un écran de détail trésorerie caisse).
     * Ne modifie pas la logique existante : réutilise les mêmes calculs que la trésorerie.
     */
    @Transactional(readOnly = true)
    public TresorerieCaissePeriodeDTO getCaisseDetailParPeriode(HttpServletRequest request, String periode,
                                                                LocalDate dateDebut, LocalDate dateFin,
                                                                int page, int size) {
        Long entrepriseId = validerEntrepriseEtPermissions(request);

        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 200) size = 200;

        // 1. Solde actuel = montantCaisse global (toutes périodes)
        TresorerieDTO tresorerieGlobale = calculerTresorerieParEntrepriseId(entrepriseId);
        double soldeActuel = tresorerieGlobale.getMontantCaisse() != null ? tresorerieGlobale.getMontantCaisse() : 0.0;

        // 2. Période
        PeriodeDates periodeDates = calculerDatesPeriode(periode, dateDebut, dateFin);
        TresorerieData dataPeriode = chargerDonneesAvecPeriode(entrepriseId, periodeDates);

        // 3. CA période (même logique que trésorerie)
        double caPeriode = calculerCAPeriode(dataPeriode, entrepriseId, periodeDates);

        // 4. Transactions de caisse sur la période
        java.util.List<TresorerieCaissePeriodeDTO.LigneTransaction> lignes = new java.util.ArrayList<>();

        // 4.1 Fermetures de caisse dans la période (entrées de caisse issues des fermetures)
        for (Caisse c : dataPeriode.caissesFermees) {
            if (c.getDateFermeture() == null) continue;
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                LocalDateTime df = c.getDateFermeture();
                if (df.isBefore(periodeDates.dateDebut) || !df.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            Double montant = c.getMontantEnMain();
            if (montant == null || montant == 0) continue;
            String designation = "Fermeture caisse - "
                    + (c.getBoutique() != null ? c.getBoutique().getNomBoutique() : "")
                    + " - "
                    + (c.getVendeur() != null ? c.getVendeur().getNomComplet() : "");
            TresorerieCaissePeriodeDTO.LigneTransaction lt =
                    new TresorerieCaissePeriodeDTO.LigneTransaction(
                            c.getDateFermeture(),
                            designation,
                            "ENTREE",
                            (double) Math.round(montant),
                            "FERMETURE_CAISSE",
                            null,
                            null
                    );
            lignes.add(lt);
        }

        // 4.2 Entrées générales CAISSE (y compris transferts, mais typées différemment)
        for (EntreeGenerale e : dataPeriode.entreesGenerales) {
            LocalDateTime dc = e.getDateCreation();
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                if (dc == null || dc.isBefore(periodeDates.dateDebut) || !dc.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            if (e.getSource() != SourceDepense.CAISSE) continue;
            // Transferts de fonds : on les affiche comme TRANSFERT, sans les compter dans le CA (déjà exclu ailleurs)
            if (estEntreeDeTransfert(e.getDesignation())) {
                Double montant = e.getMontant();
                if (montant == null || montant == 0) continue;
                String designation = e.getDesignation();
                String source = designation != null && designation.trim().startsWith("Transfert vers")
                        ? "TRANSFERT_VERS"
                        : "TRANSFERT_DEPUIS";
                String designationLower = designation != null ? designation.toLowerCase() : "";
                TresorerieCaissePeriodeDTO.LigneTransaction lt =
                        new TresorerieCaissePeriodeDTO.LigneTransaction(
                                e.getDateCreation(),
                                designation,
                                "TRANSFERT",
                                (double) Math.round(montant),
                                source,
                                // de / vers : pour une entrée de caisse, l'argent arrive en CAISSE
                                // et vient soit de BANQUE soit de MOBILE_MONEY selon la désignation
                                designationLower.contains("banque") ? "BANQUE"
                                        : designationLower.contains("mobile") ? "MOBILE_MONEY"
                                        : "INCONNU",
                                "CAISSE"
                        );
                lignes.add(lt);
                continue;
            }
            String dt = e.getDetteType();
            if (dt != null && !"ENTREE_DETTE".equals(dt)) continue;
            Double montant = e.getMontant();
            if (montant == null || montant == 0) continue;
            TresorerieCaissePeriodeDTO.LigneTransaction lt =
                    new TresorerieCaissePeriodeDTO.LigneTransaction(
                            e.getDateCreation(),
                            e.getDesignation(),
                            "ENTREE_CAISSE",
                            (double) Math.round(montant),
                            "CAISSE",
                            null,
                            null
                    );
            lignes.add(lt);
        }

        // 4.3 Paiements factures espèces de la période
        if (periodeDates != null && periodeDates.filtrerParPeriode) {
            java.util.List<Paiement> paiementsPeriode = paiementRepository.findByEntrepriseIdAndDatePaiementBetween(
                    entrepriseId, periodeDates.dateDebut, periodeDates.dateFin);
            for (Paiement p : paiementsPeriode) {
                double montant = getValeurDouble(p.getMontant());
                if (montant == 0.0) continue;
                String source = p.getModePaiement() != null ? p.getModePaiement() : "INCONNU";
                String numeroFacture = null;
                if (p.getFactureReelle() != null && p.getFactureReelle().getNumeroFacture() != null) {
                    numeroFacture = p.getFactureReelle().getNumeroFacture();
                }
                String designationPaiement = numeroFacture != null
                        ? "Paiement facture " + numeroFacture
                        : "Paiement facture";
                TresorerieCaissePeriodeDTO.LigneTransaction lt =
                        new TresorerieCaissePeriodeDTO.LigneTransaction(
                                p.getDatePaiement(),
                                designationPaiement,
                                "PAIEMENT_FACTURE",
                                (double) Math.round(montant),
                                source,
                                null,
                                null
                        );
                lignes.add(lt);
            }
        }

        // 4.4 Dépenses générales CAISSE (y compris transferts, mais typées différemment)
        for (DepenseGenerale d : dataPeriode.depensesGenerales) {
            LocalDateTime dd = d.getDateCreation();
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                if (dd == null || dd.isBefore(periodeDates.dateDebut) || !dd.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            if (d.getSource() != SourceDepense.CAISSE) continue;
            // Transferts de fonds : on les affiche comme TRANSFERT, sans les compter deux fois dans les totaux
            if (estDepenseDeTransfert(d.getDesignation())) {
                Double montant = d.getMontant();
                if (montant == null || montant == 0) continue;
                String designation = d.getDesignation();
                String source = designation != null && designation.trim().startsWith("Transfert vers")
                        ? "TRANSFERT_VERS"
                        : "TRANSFERT_DEPUIS";
                String designationLower = designation != null ? designation.toLowerCase() : "";
                TresorerieCaissePeriodeDTO.LigneTransaction lt =
                        new TresorerieCaissePeriodeDTO.LigneTransaction(
                                d.getDateCreation(),
                                designation,
                                "TRANSFERT",
                                (double) Math.round(montant),
                                source,
                                // de / vers : pour une dépense de caisse, l'argent sort de CAISSE
                                // et va soit vers BANQUE soit vers MOBILE_MONEY selon la désignation
                                "CAISSE",
                                designationLower.contains("banque") ? "BANQUE"
                                        : designationLower.contains("mobile") ? "MOBILE_MONEY"
                                        : "INCONNU"
                        );
                lignes.add(lt);
                continue;
            }
            Double montant = d.getMontant();
            if (montant == null || montant == 0) continue;
            TresorerieCaissePeriodeDTO.LigneTransaction lt =
                    new TresorerieCaissePeriodeDTO.LigneTransaction(
                            d.getDateCreation(),
                            d.getDesignation(),
                            "DEPENSE_CAISSE",
                            (double) Math.round(montant),
                            "CAISSE",
                            null,
                            null
                    );
            lignes.add(lt);
        }

        // 4.5 Mouvements caisse DEPENSE et RETRAIT
        for (MouvementCaisse m : dataPeriode.mouvementsDepense) {
            LocalDateTime dm = m.getDateMouvement();
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                if (dm == null || dm.isBefore(periodeDates.dateDebut) || !dm.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            Double montant = m.getMontant();
            if (montant == null || montant == 0) continue;
            TresorerieCaissePeriodeDTO.LigneTransaction lt =
                    new TresorerieCaissePeriodeDTO.LigneTransaction(
                            m.getDateMouvement(),
                            m.getDescription(),
                            "DEPENSE_CAISSE",
                            (double) Math.round(montant),
                            m.getModePaiement() != null ? m.getModePaiement().name() : "CAISSE",
                            null,
                            null
                    );
            lignes.add(lt);
        }
        for (MouvementCaisse m : dataPeriode.mouvementsRetrait) {
            LocalDateTime dm = m.getDateMouvement();
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                if (dm == null || dm.isBefore(periodeDates.dateDebut) || !dm.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            Double montant = m.getMontant();
            if (montant == null || montant == 0) continue;
            TresorerieCaissePeriodeDTO.LigneTransaction lt =
                    new TresorerieCaissePeriodeDTO.LigneTransaction(
                            m.getDateMouvement(),
                            m.getDescription(),
                            "RETRAIT_CAISSE",
                            (double) Math.round(montant),
                            m.getModePaiement() != null ? m.getModePaiement().name() : "CAISSE",
                            null,
                            null
                    );
            lignes.add(lt);
        }

        // Trier les transactions par date décroissante
        lignes.sort((a, b) -> {
            if (a.getDate() == null && b.getDate() == null) return 0;
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

        TresorerieCaissePeriodeDTO dto = new TresorerieCaissePeriodeDTO();
        dto.setSoldeActuel((double) Math.round(soldeActuel));
        dto.setCaPeriode((double) Math.round(caPeriode));
        dto.setNombreTransactions(lignes.size());

        // Pagination en mémoire sur les transactions (les données sont déjà filtrées par période et entreprise).
        int total = lignes.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        dto.setTransactions(lignes.subList(fromIndex, toIndex));

        // Métadonnées de pagination comme en comptabilité
        dto.setPageNumber(page);
        dto.setPageSize(size);
        dto.setTotalElements(total);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        dto.setTotalPages(totalPages);
        dto.setHasNext(page < totalPages - 1);
        dto.setHasPrevious(page > 0);
        dto.setFirst(page == 0);
        dto.setLast(page >= totalPages - 1 || totalPages == 0);
        return dto;
    }

    /**
     * Détail de la banque pour une période donnée (écran de détail trésorerie banque).
     * Ne modifie pas la logique existante : réutilise les mêmes données que la trésorerie.
     */
    @Transactional(readOnly = true)
    public TresorerieBanquePeriodeDTO getBanqueDetailParPeriode(HttpServletRequest request, String periode,
                                                                LocalDate dateDebut, LocalDate dateFin,
                                                                int page, int size) {
        Long entrepriseId = validerEntrepriseEtPermissions(request);

        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 200) size = 200;

        // 1. Solde actuel de la banque
        TresorerieDTO tresorerieGlobale = calculerTresorerieParEntrepriseId(entrepriseId);
        double soldeActuel = tresorerieGlobale.getMontantBanque() != null ? tresorerieGlobale.getMontantBanque() : 0.0;

        // 2. Période
        PeriodeDates periodeDates = calculerDatesPeriode(periode, dateDebut, dateFin);
        TresorerieData dataPeriode = chargerDonneesAvecPeriode(entrepriseId, periodeDates);

        // 3. CA période côté banque (entrées banque uniquement)
        double caPeriode = calculerCABanquePeriode(dataPeriode, entrepriseId, periodeDates);

        // 4. Transactions banque
        java.util.List<TresorerieBanquePeriodeDTO.LigneTransaction> lignes = new java.util.ArrayList<>();

        // 4.1 Ventes de la période encaissées par banque (VIREMENT, CHEQUE, CARTE)
        for (Vente v : dataPeriode.toutesVentes) {
            if (v.getStatus() == VenteStatus.REMBOURSEE) continue;
            ModePaiement mp = v.getModePaiement();
            if (mp == ModePaiement.VIREMENT || mp == ModePaiement.CHEQUE || mp == ModePaiement.CARTE) {
                double montant = getValeurDouble(v.getMontantTotal());
                if (montant == 0.0) continue;
                String designation = v.getDescription() != null ? v.getDescription() : "Vente";
                lignes.add(new TresorerieBanquePeriodeDTO.LigneTransaction(
                        v.getDateVente(),
                        designation,
                        "ENTREE_BANQUE",
                        (double) Math.round(montant),
                        mp.name(),
                        null,
                        null
                ));
            }
        }

        // 4.2 Paiements factures de la période (VIREMENT, CHEQUE, CARTE)
        if (periodeDates != null && periodeDates.filtrerParPeriode) {
            java.util.List<Paiement> paiementsPeriode = paiementRepository.findByEntrepriseIdAndDatePaiementBetween(
                    entrepriseId, periodeDates.dateDebut, periodeDates.dateFin);
            for (Paiement p : paiementsPeriode) {
                String mp = p.getModePaiement();
                if (mp == null) continue;
                String mpUpper = mp.toUpperCase();
                if (!mpUpper.equals("VIREMENT") && !mpUpper.equals("CHEQUE") && !mpUpper.equals("CARTE")) continue;
                double montant = getValeurDouble(p.getMontant());
                if (montant == 0.0) continue;
                String numeroFacture = null;
                if (p.getFactureReelle() != null && p.getFactureReelle().getNumeroFacture() != null) {
                    numeroFacture = p.getFactureReelle().getNumeroFacture();
                }
                String designationPaiement = numeroFacture != null
                        ? "Paiement facture " + numeroFacture
                        : "Paiement facture";
                lignes.add(new TresorerieBanquePeriodeDTO.LigneTransaction(
                        p.getDatePaiement(),
                        designationPaiement,
                        "PAIEMENT_FACTURE",
                        (double) Math.round(montant),
                        mpUpper,
                        null,
                        null
                ));
            }
        }

        // 4.3 Entrées générales BANQUE (hors transferts, hors paiements facture)
        for (EntreeGenerale e : dataPeriode.entreesGenerales) {
            LocalDateTime dc = e.getDateCreation();
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                if (dc == null || dc.isBefore(periodeDates.dateDebut) || !dc.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            if (e.getSource() != SourceDepense.BANQUE) continue;
            if (estEntreeDeTransfert(e.getDesignation())) continue;
            String dt = e.getDetteType();
            if (dt != null && "PAIEMENT_FACTURE".equals(dt)) continue;
            double montant = getValeurDouble(e.getMontant());
            if (montant == 0.0) continue;
            lignes.add(new TresorerieBanquePeriodeDTO.LigneTransaction(
                    e.getDateCreation(),
                    e.getDesignation(),
                    "ENTREE_BANQUE",
                    (double) Math.round(montant),
                    "BANQUE",
                    null,
                    null
            ));
        }

        // 4.4 Dépenses générales BANQUE (hors transferts)
        for (DepenseGenerale d : dataPeriode.depensesGenerales) {
            LocalDateTime dd = d.getDateCreation();
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                if (dd == null || dd.isBefore(periodeDates.dateDebut) || !dd.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            if (d.getSource() != SourceDepense.BANQUE) continue;
            if (estDepenseDeTransfert(d.getDesignation())) continue;
            double montant = getValeurDouble(d.getMontant());
            if (montant == 0.0) continue;
            lignes.add(new TresorerieBanquePeriodeDTO.LigneTransaction(
                    d.getDateCreation(),
                    d.getDesignation(),
                    "DEPENSE_BANQUE",
                    (double) Math.round(montant),
                    "BANQUE",
                    null,
                    null
            ));
        }

        // 4.5 Mouvements caisse DEPENSE / RETRAIT vers/depuis la banque (VIREMENT, CHEQUE, CARTE)
        for (MouvementCaisse m : dataPeriode.mouvementsDepense) {
            LocalDateTime dm = m.getDateMouvement();
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                if (dm == null || dm.isBefore(periodeDates.dateDebut) || !dm.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            ModePaiement mp = m.getModePaiement();
            if (mp != ModePaiement.VIREMENT && mp != ModePaiement.CHEQUE && mp != ModePaiement.CARTE) continue;
            double montant = getValeurDouble(m.getMontant());
            if (montant == 0.0) continue;
            lignes.add(new TresorerieBanquePeriodeDTO.LigneTransaction(
                    m.getDateMouvement(),
                    m.getDescription(),
                    "DEPENSE_BANQUE",
                    (double) Math.round(montant),
                    mp.name(),
                    null,
                    null
            ));
        }

        // 4.6 Transferts de fonds impliquant la BANQUE (CAISSE <-> BANQUE, BANQUE <-> MOBILE_MONEY, etc.)
        java.util.List<TransfertFonds> transferts = transfertFondsRepository.findByEntrepriseIdOrderByDateTransfertDesc(entrepriseId);
        for (TransfertFonds t : transferts) {
            LocalDateTime dt = t.getDateTransfert();
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                if (dt == null || dt.isBefore(periodeDates.dateDebut) || !dt.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            SourceTresorerie src = t.getSource();
            SourceTresorerie dest = t.getDestination();
            // On ne garde que les transferts où la banque est source ou destination
            if (src != SourceTresorerie.BANQUE && dest != SourceTresorerie.BANQUE) continue;

            double montant = getValeurDouble(t.getMontant());
            if (montant == 0.0) continue;

            String designation = "Transfert de " + src.name() + " vers " + dest.name() + " - " + t.getMotif();
            // Côté banque : si BANQUE est en destination, on peut considérer que c'est une ENTREE,
            // si BANQUE est en source, c'est une SORTIE. On garde "TRANSFERT" comme source.
            String type;
            if (dest == SourceTresorerie.BANQUE) {
                type = "ENTREE_BANQUE";
            } else if (src == SourceTresorerie.BANQUE) {
                type = "DEPENSE_BANQUE";
            } else {
                type = "TRANSFERT";
            }
            String source = "TRANSFERT";
            String de = src != null ? src.name() : null;
            String vers = dest != null ? dest.name() : null;

            lignes.add(new TresorerieBanquePeriodeDTO.LigneTransaction(
                    dt,
                    designation,
                    type,
                    (double) Math.round(montant),
                    source,
                    de,
                    vers
            ));
        }

        // Tri desc
        lignes.sort((a, b) -> {
            if (a.getDate() == null && b.getDate() == null) return 0;
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

        // 5. Pagination en mémoire
        TresorerieBanquePeriodeDTO dto = new TresorerieBanquePeriodeDTO();
        dto.setSoldeActuel((double) Math.round(soldeActuel));
        dto.setCaPeriode((double) Math.round(caPeriode));
        dto.setNombreTransactions(lignes.size());

        int total = lignes.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        dto.setTransactions(lignes.subList(fromIndex, toIndex));

        dto.setPageNumber(page);
        dto.setPageSize(size);
        dto.setTotalElements(total);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        dto.setTotalPages(totalPages);
        dto.setHasNext(page < totalPages - 1);
        dto.setHasPrevious(page > 0);
        dto.setFirst(page == 0);
        dto.setLast(page >= totalPages - 1 || totalPages == 0);
        return dto;
    }

    /**
     * CA côté banque pour la période : mêmes composantes que calculerBanque mais filtrées sur la période.
     */
    private double calculerCABanquePeriode(TresorerieData data, Long entrepriseId, PeriodeDates periodeDates) {
        double ca = 0.0;

        // Ventes banque
        for (Vente v : data.toutesVentes) {
            if (v.getStatus() == VenteStatus.REMBOURSEE) continue;
            ModePaiement mp = v.getModePaiement();
            if (mp == ModePaiement.VIREMENT || mp == ModePaiement.CHEQUE || mp == ModePaiement.CARTE) {
                ca += getValeurDouble(v.getMontantTotal());
            }
        }

        // Paiements factures banque
        if (periodeDates != null && periodeDates.filtrerParPeriode) {
            java.util.List<Paiement> paiementsPeriode = paiementRepository.findByEntrepriseIdAndDatePaiementBetween(
                    entrepriseId, periodeDates.dateDebut, periodeDates.dateFin);
            for (Paiement p : paiementsPeriode) {
                String mp = p.getModePaiement();
                if (mp == null) continue;
                String mpUpper = mp.toUpperCase();
                if (!mpUpper.equals("VIREMENT") && !mpUpper.equals("CHEQUE") && !mpUpper.equals("CARTE")) continue;
                ca += getValeurDouble(p.getMontant());
            }
        }

        // Entrées générales BANQUE (hors transferts, hors paiements facture)
        for (EntreeGenerale e : data.entreesGenerales) {
            if (e.getSource() != SourceDepense.BANQUE) continue;
            if (estEntreeDeTransfert(e.getDesignation())) continue;
            String dt = e.getDetteType();
            if (dt != null && "PAIEMENT_FACTURE".equals(dt)) continue;
            ca += getValeurDouble(e.getMontant());
        }

        return ca;
    }

    /**
     * Détail du mobile money pour une période donnée (écran de détail trésorerie mobile).
     * Ne modifie pas la logique existante : réutilise les mêmes données que la trésorerie.
     */
    @Transactional(readOnly = true)
    public TresorerieMobilePeriodeDTO getMobileDetailParPeriode(HttpServletRequest request, String periode,
                                                                LocalDate dateDebut, LocalDate dateFin,
                                                                int page, int size) {
        Long entrepriseId = validerEntrepriseEtPermissions(request);

        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 200) size = 200;

        // 1. Solde actuel du mobile money
        TresorerieDTO tresorerieGlobale = calculerTresorerieParEntrepriseId(entrepriseId);
        double soldeActuel = tresorerieGlobale.getMontantMobileMoney() != null ? tresorerieGlobale.getMontantMobileMoney() : 0.0;

        // 2. Période
        PeriodeDates periodeDates = calculerDatesPeriode(periode, dateDebut, dateFin);
        TresorerieData dataPeriode = chargerDonneesAvecPeriode(entrepriseId, periodeDates);

        // 3. CA période côté mobile (entrées mobile uniquement)
        double caPeriode = calculerCAMobilePeriode(dataPeriode, entrepriseId, periodeDates);

        // 4. Transactions mobile
        java.util.List<TresorerieMobilePeriodeDTO.LigneTransaction> lignes = new java.util.ArrayList<>();

        // 4.1 Ventes de la période encaissées par mobile money
        for (Vente v : dataPeriode.toutesVentes) {
            if (v.getStatus() == VenteStatus.REMBOURSEE) continue;
            ModePaiement mp = v.getModePaiement();
            if (mp == ModePaiement.MOBILE_MONEY) {
                double montant = getValeurDouble(v.getMontantTotal());
                if (montant == 0.0) continue;
                String designation = v.getDescription() != null ? v.getDescription() : "Vente";
                lignes.add(new TresorerieMobilePeriodeDTO.LigneTransaction(
                        v.getDateVente(),
                        designation,
                        "ENTREE_MOBILE",
                        (double) Math.round(montant),
                        "MOBILE_MONEY",
                        null,
                        null
                ));
            }
        }

        // 4.2 Paiements factures de la période via mobile money
        if (periodeDates != null && periodeDates.filtrerParPeriode) {
            java.util.List<Paiement> paiementsPeriode = paiementRepository.findByEntrepriseIdAndDatePaiementBetween(
                    entrepriseId, periodeDates.dateDebut, periodeDates.dateFin);
            for (Paiement p : paiementsPeriode) {
                String mp = p.getModePaiement();
                if (mp == null) continue;
                String mpUpper = mp.toUpperCase();
                if (!mpUpper.equals("MOBILE_MONEY")) continue;
                double montant = getValeurDouble(p.getMontant());
                if (montant == 0.0) continue;
                String numeroFacture = null;
                if (p.getFactureReelle() != null && p.getFactureReelle().getNumeroFacture() != null) {
                    numeroFacture = p.getFactureReelle().getNumeroFacture();
                }
                String designationPaiement = numeroFacture != null
                        ? "Paiement facture " + numeroFacture
                        : "Paiement facture";
                lignes.add(new TresorerieMobilePeriodeDTO.LigneTransaction(
                        p.getDatePaiement(),
                        designationPaiement,
                        "PAIEMENT_FACTURE",
                        (double) Math.round(montant),
                        mpUpper,
                        null,
                        null
                ));
            }
        }

        // 4.3 Entrées générales MOBILE_MONEY (hors transferts, hors paiements facture)
        for (EntreeGenerale e : dataPeriode.entreesGenerales) {
            LocalDateTime dc = e.getDateCreation();
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                if (dc == null || dc.isBefore(periodeDates.dateDebut) || !dc.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            if (e.getSource() != SourceDepense.MOBILE_MONEY) continue;
            if (estEntreeDeTransfert(e.getDesignation())) continue;
            String dt = e.getDetteType();
            if (dt != null && "PAIEMENT_FACTURE".equals(dt)) continue;
            double montant = getValeurDouble(e.getMontant());
            if (montant == 0.0) continue;
            lignes.add(new TresorerieMobilePeriodeDTO.LigneTransaction(
                    e.getDateCreation(),
                    e.getDesignation(),
                    "ENTREE_MOBILE",
                    (double) Math.round(montant),
                    "MOBILE_MONEY",
                    null,
                    null
            ));
        }

        // 4.4 Dépenses générales MOBILE_MONEY (hors transferts)
        for (DepenseGenerale d : dataPeriode.depensesGenerales) {
            LocalDateTime dd = d.getDateCreation();
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                if (dd == null || dd.isBefore(periodeDates.dateDebut) || !dd.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            if (d.getSource() != SourceDepense.MOBILE_MONEY) continue;
            if (estDepenseDeTransfert(d.getDesignation())) continue;
            double montant = getValeurDouble(d.getMontant());
            if (montant == 0.0) continue;
            lignes.add(new TresorerieMobilePeriodeDTO.LigneTransaction(
                    d.getDateCreation(),
                    d.getDesignation(),
                    "DEPENSE_MOBILE",
                    (double) Math.round(montant),
                    "MOBILE_MONEY",
                    null,
                    null
            ));
        }

        // 4.5 Mouvements caisse DEPENSE / RETRAIT vers/depuis le mobile money
        for (MouvementCaisse m : dataPeriode.mouvementsDepense) {
            LocalDateTime dm = m.getDateMouvement();
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                if (dm == null || dm.isBefore(periodeDates.dateDebut) || !dm.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            ModePaiement mp = m.getModePaiement();
            if (mp != ModePaiement.MOBILE_MONEY) continue;
            double montant = getValeurDouble(m.getMontant());
            if (montant == 0.0) continue;
            lignes.add(new TresorerieMobilePeriodeDTO.LigneTransaction(
                    m.getDateMouvement(),
                    m.getDescription(),
                    "DEPENSE_MOBILE",
                    (double) Math.round(montant),
                    mp.name(),
                    null,
                    null
            ));
        }

        // 4.6 Transferts de fonds impliquant le MOBILE_MONEY
        java.util.List<TransfertFonds> transferts = transfertFondsRepository.findByEntrepriseIdOrderByDateTransfertDesc(entrepriseId);
        for (TransfertFonds t : transferts) {
            LocalDateTime dt = t.getDateTransfert();
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                if (dt == null || dt.isBefore(periodeDates.dateDebut) || !dt.isBefore(periodeDates.dateFin)) {
                    continue;
                }
            }
            SourceTresorerie src = t.getSource();
            SourceTresorerie dest = t.getDestination();
            if (src != SourceTresorerie.MOBILE_MONEY && dest != SourceTresorerie.MOBILE_MONEY) continue;

            double montant = getValeurDouble(t.getMontant());
            if (montant == 0.0) continue;

            String designation = "Transfert de " + src.name() + " vers " + dest.name() + " - " + t.getMotif();
            String type;
            if (dest == SourceTresorerie.MOBILE_MONEY) {
                type = "ENTREE_MOBILE";
            } else if (src == SourceTresorerie.MOBILE_MONEY) {
                type = "DEPENSE_MOBILE";
            } else {
                type = "TRANSFERT";
            }
            String source = "TRANSFERT";
            String de = src != null ? src.name() : null;
            String vers = dest != null ? dest.name() : null;

            lignes.add(new TresorerieMobilePeriodeDTO.LigneTransaction(
                    dt,
                    designation,
                    type,
                    (double) Math.round(montant),
                    source,
                    de,
                    vers
            ));
        }

        // Tri desc
        lignes.sort((a, b) -> {
            if (a.getDate() == null && b.getDate() == null) return 0;
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

        TresorerieMobilePeriodeDTO dto = new TresorerieMobilePeriodeDTO();
        dto.setSoldeActuel((double) Math.round(soldeActuel));
        dto.setCaPeriode((double) Math.round(caPeriode));
        dto.setNombreTransactions(lignes.size());

        int total = lignes.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        dto.setTransactions(lignes.subList(fromIndex, toIndex));

        dto.setPageNumber(page);
        dto.setPageSize(size);
        dto.setTotalElements(total);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        dto.setTotalPages(totalPages);
        dto.setHasNext(page < totalPages - 1);
        dto.setHasPrevious(page > 0);
        dto.setFirst(page == 0);
        dto.setLast(page >= totalPages - 1 || totalPages == 0);
        return dto;
    }

    /**
     * CA côté mobile money pour la période.
     */
    private double calculerCAMobilePeriode(TresorerieData data, Long entrepriseId, PeriodeDates periodeDates) {
        double ca = 0.0;

        // Ventes mobile
        for (Vente v : data.toutesVentes) {
            if (v.getStatus() == VenteStatus.REMBOURSEE) continue;
            ModePaiement mp = v.getModePaiement();
            if (mp == ModePaiement.MOBILE_MONEY) {
                ca += getValeurDouble(v.getMontantTotal());
            }
        }

        // Paiements factures mobile
        if (periodeDates != null && periodeDates.filtrerParPeriode) {
            java.util.List<Paiement> paiementsPeriode = paiementRepository.findByEntrepriseIdAndDatePaiementBetween(
                    entrepriseId, periodeDates.dateDebut, periodeDates.dateFin);
            for (Paiement p : paiementsPeriode) {
                String mp = p.getModePaiement();
                if (mp == null) continue;
                String mpUpper = mp.toUpperCase();
                if (!mpUpper.equals("MOBILE_MONEY")) continue;
                ca += getValeurDouble(p.getMontant());
            }
        }

        // Entrées générales MOBILE_MONEY (hors transferts, hors paiements facture)
        for (EntreeGenerale e : data.entreesGenerales) {
            if (e.getSource() != SourceDepense.MOBILE_MONEY) continue;
            if (estEntreeDeTransfert(e.getDesignation())) continue;
            String dt = e.getDetteType();
            if (dt != null && "PAIEMENT_FACTURE".equals(dt)) continue;
            ca += getValeurDouble(e.getMontant());
        }

        return ca;
    }


     // Calcule la trésorerie pour une entreprise donnée.

    @Transactional(readOnly = true)
    public TresorerieDTO calculerTresorerieParEntrepriseId(Long entrepriseId) {
        try {
            TresorerieData data = chargerDonnees(entrepriseId);
            return calculerTresorerieDepuisData(data, entrepriseId, null);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Erreur lors du calcul de la trésorerie pour l'entreprise {}", entrepriseId, e);
            throw new BusinessException("Erreur lors du calcul de la trésorerie : " + e.getMessage());
        }
    }

    /**
     * Calcule la trésorerie pour une entreprise avec filtrage par période.
     * Les montants globaux (montantCaisse, montantBanque, etc.) sont toujours calculés avec TOUTES les données.
     * Seuls le solde et le CA sont filtrés par période.
     */
    @Transactional(readOnly = true)
    public TresorerieDTO calculerTresorerieParEntrepriseIdAvecPeriode(Long entrepriseId, PeriodeDates periodeDates) {
        try {
            // Charger TOUTES les données pour les montants globaux (montantCaisse, montantBanque, etc.)
            TresorerieData dataGlobale = chargerDonnees(entrepriseId);
            
            // Calculer les montants globaux avec toutes les données
            TresorerieDTO tresorerie = calculerTresorerieDepuisData(dataGlobale, entrepriseId, null);
            
            // Si une période est spécifiée, calculer le solde, le CA et le montant payé sur les dettes pour cette période uniquement
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                // Charger les données filtrées UNIQUEMENT pour le calcul du solde et CA de la période
                TresorerieData dataPeriode = chargerDonneesAvecPeriode(entrepriseId, periodeDates);

                // CA / solde par compte
                double caCaisse = calculerCAPeriode(dataPeriode, entrepriseId, periodeDates);
                double sortiesCaisse = calculerSortiesPeriode(dataPeriode, entrepriseId, periodeDates);
                double soldeCaisse = caCaisse - sortiesCaisse;

                double caBanque = calculerCABanquePeriode(dataPeriode, entrepriseId, periodeDates);
                double sortiesBanque = calculerSortiesBanquePeriode(dataPeriode, entrepriseId, periodeDates);
                double soldeBanque = caBanque - sortiesBanque;

                double caMobile = calculerCAMobilePeriode(dataPeriode, entrepriseId, periodeDates);
                double sortiesMobile = calculerSortiesMobilePeriode(dataPeriode, entrepriseId, periodeDates);
                double soldeMobile = caMobile - sortiesMobile;

                // Montant payé sur les dettes pendant la période (paiements factures + paiements dettes)
                TresorerieDTO.DetteDetail detteDetail = tresorerie.getDetteDetail();
                if (detteDetail != null) {
                    double montantPayePeriode = calculerMontantDettesPayePeriode(entrepriseId, periodeDates, dataPeriode);
                    detteDetail.setMontantPayePeriode((double) Math.round(montantPayePeriode));
                }

                // Renseigner les champs par compte
                tresorerie.setCaCaissePeriode(caCaisse);
                tresorerie.setSoldeCaissePeriode(soldeCaisse);
                tresorerie.setCaBanquePeriode(caBanque);
                tresorerie.setSoldeBanquePeriode(soldeBanque);
                tresorerie.setCaMobilePeriode(caMobile);
                tresorerie.setSoldeMobilePeriode(soldeMobile);

                // CA / solde globaux = on expose ceux de la CAISSE (compte principal),
                // les autres comptes ont leurs propres champs (caBanquePeriode, caMobilePeriode, etc.).
                tresorerie.setCaAujourdhui(caCaisse);
                tresorerie.setSoldeAujourdhui(soldeCaisse);
            }
            
            return tresorerie;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Erreur lors du calcul de la trésorerie pour l'entreprise {} avec période", entrepriseId, e);
            throw new BusinessException("Erreur lors du calcul de la trésorerie : " + e.getMessage());
        }
    }

    /**
     * Calcule la trésorerie à partir des données chargées.
     */
    private TresorerieDTO calculerTresorerieDepuisData(TresorerieData data, Long entrepriseId, PeriodeDates periodeDates) {
            TresorerieDTO tresorerie = new TresorerieDTO();

            TresorerieDTO.CaisseDetail caisseDetail = calculerCaisse(data);
            tresorerie.setCaisseDetail(caisseDetail);
            
            double depensesGeneralesCaisse = data.depensesGenerales.stream()
                    .filter(d -> d.getSource() == SourceDepense.CAISSE)
                    .mapToDouble(d -> getValeurDouble(d.getMontant()))
                    .sum();
            
            double entreesGeneralesCaisse = calculerEntreesGeneralesCaisse(data);
            double entreesPaiementsEspeces = calculerEntreesPaiementsFactures(data, ModePaiement.ESPECES, null);

            double montantCaisseReel = caisseDetail.getMontantTotal()
                    + entreesGeneralesCaisse
                    + entreesPaiementsEspeces
                    - depensesGeneralesCaisse;
            double montantCaisseAffiche = Math.max(0.0, montantCaisseReel);
            montantCaisseAffiche = Math.round(montantCaisseAffiche);
            tresorerie.setMontantCaisse(montantCaisseAffiche);

            TresorerieDTO.BanqueDetail banqueDetail = calculerBanque(data);
            // Arrondir les montants banque à l'unité
            if (banqueDetail != null) {
                if (banqueDetail.getEntrees() != null) {
                    banqueDetail.setEntrees((double) Math.round(banqueDetail.getEntrees()));
                }
                if (banqueDetail.getSorties() != null) {
                    banqueDetail.setSorties((double) Math.round(banqueDetail.getSorties()));
                }
                if (banqueDetail.getSolde() != null) {
                    banqueDetail.setSolde((double) Math.round(banqueDetail.getSolde()));
                }
            }
            tresorerie.setBanqueDetail(banqueDetail);
            tresorerie.setMontantBanque(banqueDetail != null && banqueDetail.getSolde() != null
                    ? banqueDetail.getSolde() : 0.0);

            TresorerieDTO.MobileMoneyDetail mobileMoneyDetail = calculerMobileMoney(data);
            // Arrondir les montants mobile money à l'unité
            if (mobileMoneyDetail != null) {
                if (mobileMoneyDetail.getEntrees() != null) {
                    mobileMoneyDetail.setEntrees((double) Math.round(mobileMoneyDetail.getEntrees()));
                }
                if (mobileMoneyDetail.getSorties() != null) {
                    mobileMoneyDetail.setSorties((double) Math.round(mobileMoneyDetail.getSorties()));
                }
                if (mobileMoneyDetail.getSolde() != null) {
                    mobileMoneyDetail.setSolde((double) Math.round(mobileMoneyDetail.getSolde()));
                }
            }
            tresorerie.setMobileMoneyDetail(mobileMoneyDetail);
            tresorerie.setMontantMobileMoney(mobileMoneyDetail != null && mobileMoneyDetail.getSolde() != null
                    ? mobileMoneyDetail.getSolde() : 0.0);

            TresorerieDTO.DetteDetail detteDetail = calculerDette(data, entrepriseId);
            // Arrondir les montants de dette à l'unité (données globales)
            if (detteDetail != null) {
                if (detteDetail.getFacturesImpayees() != null) {
                    detteDetail.setFacturesImpayees((double) Math.round(detteDetail.getFacturesImpayees()));
                }
                if (detteDetail.getDepensesDette() != null) {
                    detteDetail.setDepensesDette((double) Math.round(detteDetail.getDepensesDette()));
                }
                if (detteDetail.getTotal() != null) {
                    detteDetail.setTotal((double) Math.round(detteDetail.getTotal()));
                }
            }
            tresorerie.setDetteDetail(detteDetail);
            tresorerie.setMontantDette(detteDetail != null && detteDetail.getTotal() != null
                    ? detteDetail.getTotal() : 0.0);

            // Total trésorerie arrondi à l'unité
            double totalTresorerie = tresorerie.getMontantCaisse()
                    + tresorerie.getMontantBanque()
                    + tresorerie.getMontantMobileMoney();
            tresorerie.setTotalTresorerie((double) Math.round(totalTresorerie));

        // Calculer le solde et le CA selon la période
        if (periodeDates != null && periodeDates.filtrerParPeriode) {
            double caPeriode = calculerCAPeriode(data, entrepriseId, periodeDates);
            double sortiesPeriode = calculerSortiesPeriode(data, entrepriseId, periodeDates);
            double soldePeriode = caPeriode - sortiesPeriode;

            tresorerie.setCaAujourdhui((double) Math.round(caPeriode));
            tresorerie.setSoldeAujourdhui((double) Math.round(soldePeriode));
        } else {
            // Par défaut, calculer pour aujourd'hui
            double caAujourdhui = calculerCAAujourdhui(data, entrepriseId);
            double sortiesAujourdhui = calculerSortiesAujourdhui(data, entrepriseId);
            double soldeAujourdhui = caAujourdhui - sortiesAujourdhui;

            tresorerie.setCaAujourdhui((double) Math.round(caAujourdhui));
            tresorerie.setSoldeAujourdhui((double) Math.round(soldeAujourdhui));
        }

            return tresorerie;
    }

     /* Récupère la liste paginée des dettes (factures impayées, ventes à crédit, dépenses en DETTE). Pagination côté base (scalable).
        afficher dans Compta front */
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<DetteItemDTO> getDettesDetaillees(HttpServletRequest request, int page, int size, String search) {
        Long entrepriseId = validerEntrepriseEtPermissions(request);
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;

        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        long totalElements = (trimmedSearch == null)
                ? tresorerieDettesDetailleesRepository.countDettesDetaillees(entrepriseId)
                : tresorerieDettesDetailleesRepository.countDettesDetailleesWithSearch(entrepriseId, trimmedSearch);

        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        List<Object[]> rows = (trimmedSearch == null)
                ? tresorerieDettesDetailleesRepository.findDettesDetailleesPage(entrepriseId, size, page * size)
                : tresorerieDettesDetailleesRepository.findDettesDetailleesPageWithSearch(entrepriseId, trimmedSearch, size, page * size);

        java.util.List<Long> factureIds = new java.util.ArrayList<>();
        java.util.List<Long> depenseIds = new java.util.ArrayList<>();
        java.util.List<Long> entreeIds = new java.util.ArrayList<>();
        java.util.List<Long> venteIds = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            String typ = row[1] != null ? row[1].toString() : "";
            Long eid = row[2] != null ? ((Number) row[2]).longValue() : null;
            if (eid == null) continue;
            switch (typ) {
                case "FACTURE_IMPAYEE" -> factureIds.add(eid);
                case "DEPENSE_DETTE" -> depenseIds.add(eid);
                case "ENTREE_DETTE" -> entreeIds.add(eid);
                case "VENTE_CREDIT" -> venteIds.add(eid);
                default -> {}
            }
        }

        java.util.Map<Long, FactureReelle> facturesMap = new java.util.HashMap<>();
        if (!factureIds.isEmpty()) {
            for (FactureReelle f : factureReelleRepository.findByIdInWithDetailsForDettes(factureIds))
                facturesMap.put(f.getId(), f);
        }
        java.util.Map<Long, DepenseGenerale> depensesMap = new java.util.HashMap<>();
        if (!depenseIds.isEmpty()) {
            for (DepenseGenerale d : depenseGeneraleRepository.findByIdInWithDetails(depenseIds))
                depensesMap.put(d.getId(), d);
        }
        java.util.Map<Long, EntreeGenerale> entreesMap = new java.util.HashMap<>();
        if (!entreeIds.isEmpty()) {
            for (EntreeGenerale e : entreeGeneraleRepository.findByIdInWithDetails(entreeIds))
                entreesMap.put(e.getId(), e);
        }
        java.util.Map<Long, Vente> ventesMap = new java.util.HashMap<>();
        if (!venteIds.isEmpty()) {
            for (Vente v : venteRepository.findByIdInWithDetailsForDettes(venteIds))
                ventesMap.put(v.getId(), v);
        }

        java.util.List<DetteItemDTO> pageContent = new java.util.ArrayList<>();
        for (Object[] row : rows) {
            String typ = row[1] != null ? row[1].toString() : "";
            Long id = row[2] != null ? ((Number) row[2]).longValue() : null;
            if (id == null) continue;
            DetteItemDTO dto = switch (typ) {
                case "FACTURE_IMPAYEE" -> mapFactureToDetteItemDTO(facturesMap.get(id));
                case "DEPENSE_DETTE" -> mapDepenseToDetteItemDTO(depensesMap.get(id));
                case "ENTREE_DETTE" -> mapEntreeToDetteItemDTO(entreesMap.get(id));
                case "VENTE_CREDIT" -> mapVenteToDetteItemDTO(ventesMap.get(id));
                default -> null;
            };
            if (dto != null) pageContent.add(dto);
        }

        PaginatedResponseDTO<DetteItemDTO> response = new PaginatedResponseDTO<>();
        response.setContent(pageContent);
        response.setPageNumber(page);
        response.setPageSize(size);
        response.setTotalElements((int) totalElements);
        response.setTotalPages(totalPages);
        response.setHasNext(page < totalPages - 1);
        response.setHasPrevious(page > 0);
        response.setFirst(page == 0);
        response.setLast(page >= totalPages - 1 || totalPages == 0);
        return response;
    }

    private DetteItemDTO mapFactureToDetteItemDTO(FactureReelle facture) {
        if (facture == null) return null;
        BigDecimal totalPaye = paiementRepository.sumMontantsByFactureReelle(facture.getId());
        if (totalPaye == null) totalPaye = BigDecimal.ZERO;
        double montantRestant = facture.getTotalFacture() - totalPaye.doubleValue();
        if (montantRestant <= 0) return null;
                DetteItemDTO dto = new DetteItemDTO();
                dto.setId(facture.getId());
                dto.setType("FACTURE_IMPAYEE");
                dto.setMontantInitial(facture.getTotalFacture());
                dto.setMontantRestant(montantRestant);
        dto.setDate(facture.getDateCreationPro() != null ? facture.getDateCreationPro()
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
        return dto;
    }

    private DetteItemDTO mapDepenseToDetteItemDTO(DepenseGenerale depense) {
        if (depense == null) return null;
            DetteItemDTO dto = new DetteItemDTO();
            dto.setId(depense.getId());
            dto.setType("DEPENSE_DETTE");
            Double montant = getValeurDouble(depense.getMontant());
            dto.setMontantInitial(montant);
            dto.setMontantRestant(montant);
            dto.setDate(depense.getDateCreation());
            dto.setDescription(depense.getDesignation());
            dto.setNumero(depense.getNumero());
            if (depense.getFournisseur() != null) {
                dto.setClient(depense.getFournisseur().getNomComplet());
                dto.setContact(depense.getFournisseur().getTelephone());
            }
        return dto;
    }

    private DetteItemDTO mapEntreeToDetteItemDTO(EntreeGenerale entree) {
        if (entree == null) return null;
            DetteItemDTO dto = new DetteItemDTO();
            dto.setId(entree.getId());
            dto.setType("ENTREE_DETTE");
            Double montantInitial = getValeurDouble(entree.getMontant());
        Double montantRestant = entree.getMontantReste() != null ? getValeurDouble(entree.getMontantReste()) : montantInitial;
            dto.setMontantInitial(montantInitial);
            dto.setMontantRestant(montantRestant);
            dto.setDate(entree.getDateCreation());
            dto.setDescription(entree.getDesignation());
            dto.setNumero(entree.getNumero());
            if (entree.getResponsable() != null) {
                String nom = entree.getResponsable().getNomComplet();
                String phone = entree.getResponsable().getPhone();
                dto.setResponsable(nom);
                dto.setResponsableContact(phone);
                // Pour les écarts caisse, le responsable est aussi la "personne à qui on doit"
                dto.setClient(nom);
                dto.setContact(phone);
        }
        return dto;
    }

    /** Délègue au bon findDettesPosIds* de VenteRepository selon sortBy/sortDir (whitelisté). */
    private List<Long> findDettesPosIds(VenteRepository repo, Long entrepriseId, LocalDateTime dateDebut, LocalDateTime dateFin,
                                       Long vendeurId, Long boutiqueId, String sortBy, String sortDir, int limit, int offset) {
        String sb = sortBy != null ? sortBy.toLowerCase() : "date";
        boolean desc = "desc".equalsIgnoreCase(sortDir != null ? sortDir : "desc");
        return switch (sb) {
            case "vendeur" -> desc ? repo.findDettesPosIdsOrderByVendeurDesc(entrepriseId, dateDebut, dateFin, vendeurId, boutiqueId, limit, offset)
                    : repo.findDettesPosIdsOrderByVendeurAsc(entrepriseId, dateDebut, dateFin, vendeurId, boutiqueId, limit, offset);
            case "boutique", "boutiquenom" -> desc ? repo.findDettesPosIdsOrderByBoutiqueDesc(entrepriseId, dateDebut, dateFin, vendeurId, boutiqueId, limit, offset)
                    : repo.findDettesPosIdsOrderByBoutiqueAsc(entrepriseId, dateDebut, dateFin, vendeurId, boutiqueId, limit, offset);
            case "montantrestant", "montant" -> desc ? repo.findDettesPosIdsOrderByMontantDesc(entrepriseId, dateDebut, dateFin, vendeurId, boutiqueId, limit, offset)
                    : repo.findDettesPosIdsOrderByMontantAsc(entrepriseId, dateDebut, dateFin, vendeurId, boutiqueId, limit, offset);
            default -> desc ? repo.findDettesPosIdsOrderByDateDesc(entrepriseId, dateDebut, dateFin, vendeurId, boutiqueId, limit, offset)
                    : repo.findDettesPosIdsOrderByDateAsc(entrepriseId, dateDebut, dateFin, vendeurId, boutiqueId, limit, offset);
        };
    }

     // Récupère uniquement les dettes issues du POS (ventes à crédit). Pagination côté base (scalable).
    
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<DetteItemDTO> getDettesPos(HttpServletRequest request, int page, int size,
            String periode, LocalDate dateDebut, LocalDate dateFin,
            String sortBy, String sortDir, Long vendeurId, Long boutiqueId) {
        Long entrepriseId = validerEntrepriseEtPermissions(request);
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100;

        // Période : dates ou null pour "toutes"
        java.time.LocalDateTime dateDebutLdt = null;
        java.time.LocalDateTime dateFinLdt = null;
        if (periode != null && !periode.trim().isEmpty() && !"toutes".equalsIgnoreCase(periode.trim())) {
            PeriodeDates periodeDates = calculerDatesPeriode(periode, dateDebut, dateFin);
            if (periodeDates.filtrerParPeriode) {
                dateDebutLdt = periodeDates.dateDebut;
                dateFinLdt = periodeDates.dateFin;
            }
        }

        long totalElements = venteRepository.countDettesPos(
                entrepriseId, dateDebutLdt, dateFinLdt, vendeurId, boutiqueId);
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        List<Long> ids = findDettesPosIds(venteRepository, entrepriseId, dateDebutLdt, dateFinLdt, vendeurId, boutiqueId,
                sortBy, sortDir, size, page * size);

        java.util.List<DetteItemDTO> pageContent = new java.util.ArrayList<>();
        if (!ids.isEmpty()) {
            List<Vente> ventes = venteRepository.findByIdInWithDetailsForDettes(ids);
            java.util.Map<Long, Vente> ventesById = ventes.stream().collect(Collectors.toMap(Vente::getId, v -> v));
            // Chargement en lot des numéros de facture (évite N+1)
            java.util.Map<Long, String> numeroByVenteId = factureVenteRepository.findByVenteIdInAndEntrepriseId(ids, entrepriseId)
                    .stream()
                    .collect(Collectors.toMap(f -> f.getVente().getId(), FactureVente::getNumeroFacture, (a, b) -> a));
            for (Long id : ids) {
                Vente v = ventesById.get(id);
                if (v != null) {
                    DetteItemDTO dto = mapVenteToDetteItemDTO(v, numeroByVenteId);
                    if (dto != null) pageContent.add(dto);
                }
            }
        }

        PaginatedResponseDTO<DetteItemDTO> response = new PaginatedResponseDTO<>();
        response.setContent(pageContent);
        response.setPageNumber(page);
        response.setPageSize(size);
        response.setTotalElements((int) totalElements);
        response.setTotalPages(totalPages);
        response.setHasNext(page < totalPages - 1);
        response.setHasPrevious(page > 0);
        response.setFirst(page == 0);
        response.setLast(page >= totalPages - 1 || totalPages == 0);
        return response;
    }

    private DetteItemDTO mapVenteToDetteItemDTO(Vente v) {
        return mapVenteToDetteItemDTO(v, null);
    }

    private DetteItemDTO mapVenteToDetteItemDTO(Vente v, java.util.Map<Long, String> numeroByVenteId) {
            double total = getValeurDouble(v.getMontantTotal());
            double rembourse = getValeurDouble(v.getMontantTotalRembourse());
            double restant = total - rembourse;
        if (restant <= 0) return null;

            DetteItemDTO dto = new DetteItemDTO();
            dto.setId(v.getId());
            dto.setType("VENTE_CREDIT");
            dto.setMontantInitial(total);
            dto.setMontantRestant(restant);
            dto.setDate(v.getDateVente());
            dto.setDescription(v.getDescription());
            double remiseGlobaleVal = v.getRemiseGlobale() != null ? v.getRemiseGlobale() : 0.0;
            dto.setRemiseGlobale(remiseGlobaleVal);
            dto.setVendeurId(v.getVendeur() != null ? v.getVendeur().getId() : null);
            dto.setVendeurNom(v.getVendeur() != null ? v.getVendeur().getNomComplet() : null);
            dto.setBoutiqueId(v.getBoutique() != null ? v.getBoutique().getId() : null);
            dto.setBoutiqueNom(v.getBoutique() != null ? v.getBoutique().getNomBoutique() : null);
            if (numeroByVenteId != null && numeroByVenteId.containsKey(v.getId())) {
                dto.setNumero(numeroByVenteId.get(v.getId()));
            } else {
                Long venteEntrepriseId = v.getBoutique() != null && v.getBoutique().getEntreprise() != null
                        ? v.getBoutique().getEntreprise().getId() : null;
                if (venteEntrepriseId != null) {
                    factureVenteRepository.findByVenteIdAndEntrepriseId(v.getId(), venteEntrepriseId)
                            .ifPresent(f -> dto.setNumero(f.getNumeroFacture()));
                }
            }
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
            if (v.getProduits() != null && !v.getProduits().isEmpty()) {
                java.util.List<DetteItemDTO.LigneProduitDetteDTO> lignes = v.getProduits().stream()
                        .map(vp -> {
                            DetteItemDTO.LigneProduitDetteDTO ligne = new DetteItemDTO.LigneProduitDetteDTO();
                            ligne.setProduitId(vp.getProduit() != null ? vp.getProduit().getId() : null);
                            ligne.setNomProduit(vp.getProduit() != null ? vp.getProduit().getNom() : null);
                            ligne.setQuantite(vp.getQuantite());
                            ligne.setPrixUnitaire(vp.getPrixUnitaire());
                        ligne.setRemise(vp.getRemise());
                            ligne.setMontantLigne(vp.getMontantLigne());
                            return ligne;
                        })
                        .collect(Collectors.toList());
                dto.setProduits(lignes);
            if (remiseGlobaleVal > 0) dto.setTypeRemise("GLOBALE");
            else if (lignes.stream().anyMatch(l -> l.getRemise() != null && l.getRemise() > 0)) {
                    dto.setTypeRemise("PAR_LIGNE");
                    dto.setRemiseGlobale(0.0);
            } else dto.setTypeRemise(null);
            } else {
                dto.setProduits(java.util.Collections.emptyList());
                dto.setTypeRemise(remiseGlobaleVal > 0 ? "GLOBALE" : null);
            }
        return dto;
    }

     // Valide l'authentification, l'appartenance à une entreprise et les permissions pour accéder à la trésorerie.

    private Long validerEntrepriseEtPermissions(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        if (user.getEntreprise() == null) {
            throw new BusinessException("Vous n'êtes associé à aucune entreprise.");
        }

        Long entrepriseId = user.getEntreprise().getId();
        
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entrepriseId);
        boolean isComptable = user.getRole() != null && user.getRole().getName() == RoleType.COMPTABLE;
        boolean hasPermission = user.getRole() != null && user.getRole().hasPermission(PermissionType.COMPTABILITE);

        if (!isAdminOrManager && !isComptable && !hasPermission) {
            throw new BusinessException("Accès refusé : vous n'avez pas les droits nécessaires pour accéder à la trésorerie.");
        }
        
        return entrepriseId;
    }

    /**
     * Calcule les dates de début et fin selon le type de période.
     * Si dateDebut et dateFin sont fournis (ex. période personnalisée), ils sont utilisés.
     */
    private PeriodeDates calculerDatesPeriode(String periode, LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut != null && dateFin != null) {
            return new PeriodeDates(dateDebut.atStartOfDay(), dateFin.plusDays(1).atStartOfDay(), true);
        }
        if (periode == null || periode.trim().isEmpty()) {
            periode = "aujourdhui";
        }
        
        LocalDateTime dateStart;
        LocalDateTime dateEnd;
        boolean filtrerParPeriode = true;

        switch (periode.toLowerCase()) {
            case "aujourdhui":
                dateStart = LocalDate.now().atStartOfDay();
                dateEnd = dateStart.plusDays(1);
                break;
            case "hier":
                dateStart = LocalDate.now().minusDays(1).atStartOfDay();
                dateEnd = dateStart.plusDays(1);
                break;
            case "semaine":
                LocalDate aujourdhui = LocalDate.now();
                dateStart = aujourdhui.minusDays(aujourdhui.getDayOfWeek().getValue() - 1).atStartOfDay();
                dateEnd = dateStart.plusWeeks(1);
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
                    throw new BusinessException("Les dates de début et de fin sont requises pour une période personnalisée.");
                }
                dateStart = dateDebut.atStartOfDay();
                dateEnd = dateFin.plusDays(1).atStartOfDay();
                break;
            default:
                // Par défaut, pas de filtre (toutes les données)
                filtrerParPeriode = false;
                dateStart = null;
                dateEnd = null;
        }

        return new PeriodeDates(dateStart, dateEnd, filtrerParPeriode);
    }

    /**
     * Classe pour stocker les dates de début et fin d'une période.
     */
    private static class PeriodeDates {
        final LocalDateTime dateDebut;
        final LocalDateTime dateFin;
        final boolean filtrerParPeriode;

        PeriodeDates(LocalDateTime dateDebut, LocalDateTime dateFin, boolean filtrerParPeriode) {
            this.dateDebut = dateDebut;
            this.dateFin = dateFin;
            this.filtrerParPeriode = filtrerParPeriode;
        }
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

     // Charge toutes les données nécessaires pour le calcul de la trésorerie.

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

        // Montant total caisse = somme des montants réellement fermés (montantEnMain)
        double montantTotalCaisse = calculerMontantTotalCaisses(data.caissesFermees);

        // Entrées complémentaires : paiements factures espèces + entrées générales CAISSE (hors transferts, hors paiements facture)
        double entreesPaiementsEspeces = calculerEntreesPaiementsFactures(data, ModePaiement.ESPECES, null);
        double entreesGeneralesCaisse = calculerEntreesGeneralesCaisse(data);

        double entrees = montantTotalCaisse + entreesGeneralesCaisse + entreesPaiementsEspeces;
        double sorties = calculerSortiesCaisse(data);

        TresorerieDTO.CaisseDetail detail = new TresorerieDTO.CaisseDetail();
        detail.setNombreCaissesOuvertes(data.caissesFermees.size());
        detail.setMontantTotal(montantTotalCaisse);
        detail.setEntrees(entrees);
        detail.setSorties(sorties);
        return detail;
    }

    private double calculerMontantTotalCaisses(List<Caisse> caisses) {
        // Somme des montants réellement fermés (montantEnMain), en excluant les fermetures à 0
        return caisses.stream()
                .filter(c -> c.getMontantEnMain() != null && c.getMontantEnMain() != 0)
                .mapToDouble(Caisse::getMontantEnMain)
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

        double entreesPaiements = calculerEntreesPaiementsFactures(data, ModePaiement.MOBILE_MONEY, null);
        double entreesGenerales = calculerEntreesGeneralesParSource(data, SourceDepense.MOBILE_MONEY);
        double entrees = entreesPaiements + entreesGenerales;
        
        double sortiesDepenses = data.depensesGenerales.stream()
                .filter(d -> d.getSource() == SourceDepense.MOBILE_MONEY)
                .mapToDouble(d -> getValeurDouble(d.getMontant()))
                .sum();

        double sortiesMouvements = data.mouvementsDepense.stream()
                .filter(m -> m.getModePaiement() == ModePaiement.MOBILE_MONEY)
                .mapToDouble(m -> getValeurDouble(m.getMontant()))
                .sum();

        double sorties = sortiesDepenses + sortiesMouvements;

        TresorerieDTO.MobileMoneyDetail mobileMoneyDetail = new TresorerieDTO.MobileMoneyDetail();
        mobileMoneyDetail.setEntrees(entrees);
        mobileMoneyDetail.setSorties(sorties);
        mobileMoneyDetail.setSolde(entrees - sorties);
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


        List<EntreeGenerale> entreesDette = data.entreesGenerales.stream()
                .filter(e -> e.getSource() == SourceDepense.DETTE)
                .filter(e -> e.getDetteType() == null || !"PAIEMENT_FACTURE".equals(e.getDetteType()))
                .collect(Collectors.toList());

        double montantEntreesDette = entreesDette.stream()
                .mapToDouble(e -> {
                    Double reste = e.getMontantReste() != null ? e.getMontantReste() : e.getMontant();
                    return getValeurDouble(reste);
                })
                .sum();

        // Filtrer uniquement les ventes à crédit des caisses fermées
        // data.toutesVentes contient déjà uniquement les ventes des caisses fermées
        List<Vente> ventesCredit = data.toutesVentes.stream()
                .filter(v -> v.getModePaiement() == ModePaiement.CREDIT)
                .collect(Collectors.toList());
        logger.info("Ventes à crédit trouvées pour l'entreprise {} (caisses fermées uniquement) : {}", entrepriseId, ventesCredit.size());

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
        detail.setDepensesDette(montantDepensesDette + montantEntreesDette);
        detail.setNombreDepensesDette(depensesDette.size() + entreesDette.size());
        detail.setTotal(totalFacturesEtCredits + montantDepensesDette + montantEntreesDette);
        return detail;
    }

    /**
     * Calcule le montant total payé sur les dettes pendant la période
     * (paiements de factures + paiements de dettes type ENTREE_DETTE : écart caisse, dépense à crédit, etc.).
     */
    private double calculerMontantDettesPayePeriode(Long entrepriseId, PeriodeDates periodeDates, TresorerieData data) {
        double total = 0.0;
        // Paiements de factures dans la période
        java.util.List<Paiement> paiements = paiementRepository.findByEntrepriseIdAndDatePaiementBetween(
                entrepriseId, periodeDates.dateDebut, periodeDates.dateFin);
        for (Paiement p : paiements) {
            total += getValeurDouble(p.getMontant());
        }
        // Paiements de dettes (ENTREE_DETTE : écart caisse, etc.) dans la période — data.entreesGenerales est déjà filtré par période
        total += data.entreesGenerales.stream()
                .filter(e -> "ENTREE_DETTE".equals(e.getDetteType()))
                .mapToDouble(e -> getValeurDouble(e.getMontant()))
                .sum();
        return total;
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
                .filter(e -> e.getDetteType() == null || !"PAIEMENT_FACTURE".equals(e.getDetteType()))
                .mapToDouble(e -> getValeurDouble(e.getMontant()))
                .sum();
    }

    private double calculerEntreesGeneralesParSource(TresorerieData data, SourceDepense sourceDepense) {

        return data.entreesGenerales.stream()
                .filter(e -> e.getSource() == sourceDepense)
                .filter(e -> e.getDetteType() == null || !"PAIEMENT_FACTURE".equals(e.getDetteType()))
                .mapToDouble(e -> getValeurDouble(e.getMontant()))
                .sum();
    }

    private double getValeurDouble(Double value) {
        return value != null ? value : 0.0;
    }

    private double getValeurDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    /**
     * Vérifie si une entrée générale provient d'un transfert de fonds.
     */
    private boolean estEntreeDeTransfert(String designation) {
        if (designation == null || designation.trim().isEmpty()) {
            return false;
        }
        return designation.trim().startsWith("Transfert vers") || designation.trim().startsWith("Transfert depuis");
    }

    /**
     * Vérifie si une dépense générale provient d'un transfert de fonds.
     */
    private boolean estDepenseDeTransfert(String designation) {
        if (designation == null || designation.trim().isEmpty()) {
            return false;
        }
        return designation.trim().startsWith("Transfert vers") || designation.trim().startsWith("Transfert depuis");
    }

    /**
     * Vérifie si une date est aujourd'hui.
     */
    private boolean estAujourdhui(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        return dateTime.toLocalDate().equals(LocalDate.now());
    }

    /**
     * Calcule le CA (Chiffre d'Affaires) d'aujourd'hui.
     * Inclut : fermetures de caisse d'aujourd'hui (montantEnMain) + paiements factures d'aujourd'hui + entrées générales CAISSE d'aujourd'hui.
     * Exclut : transferts de fonds, paiements de factures déjà comptés ailleurs et écarts de caisse (ECART_CAISSE).
     */
    private double calculerCAAujourdhui(TresorerieData data, Long entrepriseId) {
        double ca = 0.0;

        // Fermetures de caisse d'aujourd'hui : montant réel (montantEnMain)
        for (Caisse c : data.caissesFermees) {
            if (estAujourdhui(c.getDateFermeture())
                    && c.getMontantEnMain() != null
                    && c.getMontantEnMain() != 0) {
                ca += c.getMontantEnMain();
            }
        }

        // Paiements factures d'aujourd'hui (tous modes)
        List<Paiement> paiementsAujourdhui = paiementRepository.findByEntrepriseId(entrepriseId).stream()
                .filter(p -> estAujourdhui(p.getDatePaiement()))
                .collect(Collectors.toList());
        for (Paiement paiement : paiementsAujourdhui) {
            ca += getValeurDouble(paiement.getMontant());
        }

        // Entrées générales CAISSE d'aujourd'hui (hors transferts, hors paiements facture, hors écarts caisse)
        for (EntreeGenerale entree : data.entreesGenerales) {
            if (estAujourdhui(entree.getDateCreation())
                    && entree.getSource() == SourceDepense.CAISSE
                    && !estEntreeDeTransfert(entree.getDesignation())
                    && (entree.getDetteType() == null || "ENTREE_DETTE".equals(entree.getDetteType()))) {
                ca += getValeurDouble(entree.getMontant());
            }
        }

        return ca;
    }

    /**
     * Calcule les sorties d'aujourd'hui.
     * Version centrée CAISSE : on ne prend en compte que ce qui sort réellement de la caisse,
     * pas les dépenses payées directement par BANQUE ou MOBILE_MONEY (déjà couvertes par les soldes banque/mobile).
     * Inclut : dépenses générales source=CAISSE + mouvements caisse DEPENSE + retraits d'aujourd'hui.
     * Exclut : les transferts de fonds et les dépenses BANQUE / MOBILE_MONEY.
     */
    private double calculerSortiesAujourdhui(TresorerieData data, Long entrepriseId) {
        double sorties = 0.0;

        // Dépenses générales d'aujourd'hui payées PAR LA CAISSE (source = CAISSE), en excluant les transferts
        for (DepenseGenerale depense : data.depensesGenerales) {
            if (estAujourdhui(depense.getDateCreation()) 
                    && depense.getSource() == SourceDepense.CAISSE
                    && !estDepenseDeTransfert(depense.getDesignation())) {
                sorties += getValeurDouble(depense.getMontant());
            }
        }

        // Mouvements caisse DEPENSE d'aujourd'hui
        List<MouvementCaisse> mouvementsDepenseAujourdhui = mouvementCaisseRepository
                .findByCaisse_Boutique_Entreprise_IdAndTypeMouvement(entrepriseId, TypeMouvementCaisse.DEPENSE)
                .stream()
                .filter(m -> estAujourdhui(m.getDateMouvement()))
                .collect(Collectors.toList());

        for (MouvementCaisse mouvement : mouvementsDepenseAujourdhui) {
            sorties += getValeurDouble(mouvement.getMontant());
        }

        // Retraits d'aujourd'hui
        List<MouvementCaisse> mouvementsRetraitAujourdhui = mouvementCaisseRepository
                .findByCaisse_Boutique_Entreprise_IdAndTypeMouvement(entrepriseId, TypeMouvementCaisse.RETRAIT)
                .stream()
                .filter(m -> estAujourdhui(m.getDateMouvement()))
                .collect(Collectors.toList());

        for (MouvementCaisse mouvement : mouvementsRetraitAujourdhui) {
            sorties += getValeurDouble(mouvement.getMontant());
        }

        return sorties;
    }

    /**
     * Charge les données avec filtrage par période (optimisé).
     */
    private TresorerieData chargerDonneesAvecPeriode(Long entrepriseId, PeriodeDates periodeDates) {
        List<Boutique> boutiques = boutiqueRepository.findByEntrepriseId(entrepriseId);
        List<Long> boutiqueIds = boutiques.stream()
                .map(Boutique::getId)
                .collect(Collectors.toList());

        List<Caisse> caissesFermees = chargerCaissesFermees(entrepriseId);
        List<Long> caisseIdsFermees = caissesFermees.stream().map(Caisse::getId).collect(Collectors.toList());
        
        // Charger les ventes selon la période
        List<Vente> toutesVentes;
        if (periodeDates.filtrerParPeriode) {
            toutesVentes = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
                    entrepriseId, periodeDates.dateDebut, periodeDates.dateFin);
        } else {
            toutesVentes = venteRepository.findAllByEntrepriseId(entrepriseId);
        }
        
        List<Vente> ventesCaissesFermees = toutesVentes.stream()
                .filter(v -> v.getCaisse() != null && caisseIdsFermees.contains(v.getCaisse().getId()))
                .collect(Collectors.toList());
        Map<Long, Double> remboursementsParVente = calculerRemboursementsParVente(ventesCaissesFermees);
        
        List<FactureReelle> factures = factureReelleRepository.findByEntrepriseId(entrepriseId);
        Map<Long, BigDecimal> paiementsParFacture = chargerPaiementsParFacture(factures);
        
        // Charger les dépenses et entrées selon la période
        List<DepenseGenerale> depensesGenerales;
        List<EntreeGenerale> entreesGenerales;
        if (periodeDates.filtrerParPeriode) {
            depensesGenerales = depenseGeneraleRepository.findByEntrepriseIdAndDateCreationBetween(
                    entrepriseId, periodeDates.dateDebut, periodeDates.dateFin);
            entreesGenerales = entreeGeneraleRepository.findByEntrepriseIdAndDateCreationBetween(
                    entrepriseId, periodeDates.dateDebut, periodeDates.dateFin);
        } else {
            depensesGenerales = depenseGeneraleRepository.findByEntrepriseId(entrepriseId);
            entreesGenerales = entreeGeneraleRepository.findByEntrepriseId(entrepriseId);
        }
        
        // Charger les mouvements selon la période
        List<MouvementCaisse> tousMouvementsDepense;
        List<MouvementCaisse> tousMouvementsRetrait;
        if (periodeDates.filtrerParPeriode) {
            tousMouvementsDepense = mouvementCaisseRepository
                    .findByCaisse_Boutique_Entreprise_IdAndTypeMouvementAndDateMouvementBetween(
                            entrepriseId, TypeMouvementCaisse.DEPENSE, periodeDates.dateDebut, periodeDates.dateFin);
            tousMouvementsRetrait = mouvementCaisseRepository
                    .findByCaisse_Boutique_Entreprise_IdAndTypeMouvementAndDateMouvementBetween(
                            entrepriseId, TypeMouvementCaisse.RETRAIT, periodeDates.dateDebut, periodeDates.dateFin);
        } else {
            tousMouvementsDepense = mouvementCaisseRepository
                    .findByCaisse_Boutique_Entreprise_IdAndTypeMouvement(entrepriseId, TypeMouvementCaisse.DEPENSE);
            tousMouvementsRetrait = mouvementCaisseRepository
                    .findByCaisse_Boutique_Entreprise_IdAndTypeMouvement(entrepriseId, TypeMouvementCaisse.RETRAIT);
        }
        
        List<MouvementCaisse> mouvementsDepense = tousMouvementsDepense.stream()
                .filter(m -> m.getCaisse() != null && caisseIdsFermees.contains(m.getCaisse().getId()))
                .collect(Collectors.toList());
        
        List<MouvementCaisse> mouvementsRetrait = tousMouvementsRetrait.stream()
                .filter(m -> m.getCaisse() != null && caisseIdsFermees.contains(m.getCaisse().getId()))
                .collect(Collectors.toList());

        return new TresorerieData(boutiqueIds, caissesFermees, ventesCaissesFermees,
                remboursementsParVente, factures, paiementsParFacture, depensesGenerales, entreesGenerales,
                mouvementsDepense, mouvementsRetrait);
    }

    /**
     * Calcule le CA (Chiffre d'Affaires) pour une période donnée.
     * Inclut : fermetures de caisse de la période (montantEnMain) + paiements factures de la période + entrées générales CAISSE de la période.
     * Exclut : transferts de fonds, paiements de factures déjà comptés ailleurs et écarts de caisse (ECART_CAISSE).
     */
    private double calculerCAPeriode(TresorerieData data, Long entrepriseId, PeriodeDates periodeDates) {
        double ca = 0.0;

        // Fermetures de caisse dans la période : montant réel (montantEnMain)
        for (Caisse c : data.caissesFermees) {
            LocalDateTime df = c.getDateFermeture();
            if (df != null
                    && (periodeDates == null || !periodeDates.filtrerParPeriode
                        || (!df.isBefore(periodeDates.dateDebut) && df.isBefore(periodeDates.dateFin)))
                    && c.getMontantEnMain() != null
                    && c.getMontantEnMain() != 0) {
                ca += c.getMontantEnMain();
            }
        }

        // Paiements factures de la période (tous modes)
        if (periodeDates != null && periodeDates.filtrerParPeriode) {
            List<Paiement> paiementsPeriode = paiementRepository.findByEntrepriseIdAndDatePaiementBetween(
                    entrepriseId, periodeDates.dateDebut, periodeDates.dateFin);
            for (Paiement paiement : paiementsPeriode) {
                ca += getValeurDouble(paiement.getMontant());
            }
        } else {
            // Si pas de filtre explicite, on considère tous les paiements
            for (Paiement paiement : paiementRepository.findByEntrepriseId(entrepriseId)) {
                ca += getValeurDouble(paiement.getMontant());
            }
        }

        // Entrées générales CAISSE de la période (hors transferts, hors paiements facture, hors écarts caisse)
        for (EntreeGenerale entree : data.entreesGenerales) {
            LocalDateTime dc = entree.getDateCreation();
            boolean dansPeriode = (periodeDates == null || !periodeDates.filtrerParPeriode
                    || (dc != null && !dc.isBefore(periodeDates.dateDebut) && dc.isBefore(periodeDates.dateFin)));

            if (dansPeriode
                    && entree.getSource() == SourceDepense.CAISSE
                    && !estEntreeDeTransfert(entree.getDesignation())
                    && (entree.getDetteType() == null || "ENTREE_DETTE".equals(entree.getDetteType()))) {
                ca += getValeurDouble(entree.getMontant());
            }
        }

        return ca;
    }

    /**
     * Calcule les sorties pour une période donnée.
     * Version centrée CAISSE pour être cohérente avec soldeAujourdhui :
     * - on ne prend en compte que ce qui sort réellement de la caisse
     *   (dépenses générales source = CAISSE, mouvements DEPENSE et RETRAIT),
     * - on exclut les transferts de fonds et les dépenses payées directement par BANQUE / MOBILE_MONEY.
     */
    private double calculerSortiesPeriode(TresorerieData data, Long entrepriseId, PeriodeDates periodeDates) {
        double sorties = 0.0;

        // Dépenses générales de la période payées PAR LA CAISSE (source = CAISSE), en excluant les transferts
        for (DepenseGenerale depense : data.depensesGenerales) {
            if (depense.getSource() == SourceDepense.CAISSE
                    && !estDepenseDeTransfert(depense.getDesignation())) {
                sorties += getValeurDouble(depense.getMontant());
            }
        }

        // Mouvements caisse DEPENSE de la période
        for (MouvementCaisse mouvement : data.mouvementsDepense) {
            sorties += getValeurDouble(mouvement.getMontant());
        }

        // Retraits de la période
        for (MouvementCaisse mouvement : data.mouvementsRetrait) {
            sorties += getValeurDouble(mouvement.getMontant());
        }

        return sorties;
    }

    /**
     * Sorties côté banque pour la période (dépenses BANQUE + mouvements caisse vers/depuis la banque).
     */
    private double calculerSortiesBanquePeriode(TresorerieData data, Long entrepriseId, PeriodeDates periodeDates) {
        double sorties = 0.0;

        // Dépenses générales BANQUE (hors transferts)
        for (DepenseGenerale depense : data.depensesGenerales) {
            if (depense.getSource() == SourceDepense.BANQUE
                    && !estDepenseDeTransfert(depense.getDesignation())) {
                sorties += getValeurDouble(depense.getMontant());
            }
        }

        // Mouvements caisse DEPENSE vers la banque (VIREMENT, CHEQUE, CARTE)
        for (MouvementCaisse mouvement : data.mouvementsDepense) {
            ModePaiement mp = mouvement.getModePaiement();
            if (mp == ModePaiement.VIREMENT || mp == ModePaiement.CHEQUE || mp == ModePaiement.CARTE) {
                sorties += getValeurDouble(mouvement.getMontant());
            }
        }

        return sorties;
    }

    /**
     * Sorties côté mobile money pour la période (dépenses MOBILE_MONEY + mouvements caisse vers mobile).
     */
    private double calculerSortiesMobilePeriode(TresorerieData data, Long entrepriseId, PeriodeDates periodeDates) {
        double sorties = 0.0;

        // Dépenses générales MOBILE_MONEY (hors transferts)
        for (DepenseGenerale depense : data.depensesGenerales) {
            if (depense.getSource() == SourceDepense.MOBILE_MONEY
                    && !estDepenseDeTransfert(depense.getDesignation())) {
                sorties += getValeurDouble(depense.getMontant());
            }
        }

        // Mouvements caisse DEPENSE vers le mobile money
        for (MouvementCaisse mouvement : data.mouvementsDepense) {
            ModePaiement mp = mouvement.getModePaiement();
            if (mp == ModePaiement.MOBILE_MONEY) {
                sorties += getValeurDouble(mouvement.getMontant());
            }
        }

        return sorties;
    }
}
