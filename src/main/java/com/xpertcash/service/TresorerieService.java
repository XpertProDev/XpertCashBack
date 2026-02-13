package com.xpertcash.service;

import com.xpertcash.DTOs.DetteItemDTO;
import com.xpertcash.DTOs.PaginatedResponseDTO;
import com.xpertcash.DTOs.TresorerieDTO;
import com.xpertcash.configuration.CentralAccess;
import com.xpertcash.entity.*;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.SourceDepense;
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

     // Calcule la trésorerie complète de l'entreprise de l'utilisateur connecté.
    @Transactional(readOnly = true)
    public TresorerieDTO calculerTresorerie(HttpServletRequest request) {
        Long entrepriseId = validerEntrepriseEtPermissions(request);
        return calculerTresorerieParEntrepriseId(entrepriseId);
    }

    /**
     * Calcule la trésorerie avec filtrage par période.
     * @param request Requête HTTP
     * @param periode Type de période : "aujourdhui", "hier", "semaine", "mois", "annee", "personnalise"
     * @param dateDebut Date de début (requis si periode = "personnalise")
     * @param dateFin Date de fin (requis si periode = "personnalise")
     * @return TresorerieDTO avec les données filtrées par période
     */
    @Transactional(readOnly = true)
    public TresorerieDTO calculerTresorerie(HttpServletRequest request, String periode, LocalDate dateDebut, LocalDate dateFin) {
        Long entrepriseId = validerEntrepriseEtPermissions(request);
        
        // Calculer les dates selon la période
        PeriodeDates periodeDates = calculerDatesPeriode(periode, dateDebut, dateFin);
        
        return calculerTresorerieParEntrepriseIdAvecPeriode(entrepriseId, periodeDates);
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
            
            // Si une période est spécifiée, calculer le solde et CA pour cette période uniquement
            if (periodeDates != null && periodeDates.filtrerParPeriode) {
                // Charger les données filtrées UNIQUEMENT pour le calcul du solde et CA de la période
                TresorerieData dataPeriode = chargerDonneesAvecPeriode(entrepriseId, periodeDates);
                
                double caPeriode = calculerCAPeriode(dataPeriode, entrepriseId, periodeDates);
                double sortiesPeriode = calculerSortiesPeriode(dataPeriode, entrepriseId, periodeDates);
                double soldePeriode = caPeriode - sortiesPeriode;
                
                tresorerie.setCaAujourdhui(caPeriode);
                tresorerie.setSoldeAujourdhui(soldePeriode);
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

        // Calculer le solde et le CA selon la période
        if (periodeDates != null && periodeDates.filtrerParPeriode) {
            double caPeriode = calculerCAPeriode(data, entrepriseId, periodeDates);
            double sortiesPeriode = calculerSortiesPeriode(data, entrepriseId, periodeDates);
            double soldePeriode = caPeriode - sortiesPeriode;
            
            tresorerie.setCaAujourdhui(caPeriode);
            tresorerie.setSoldeAujourdhui(soldePeriode);
        } else {
            // Par défaut, calculer pour aujourd'hui
            double caAujourdhui = calculerCAAujourdhui(data, entrepriseId);
            double sortiesAujourdhui = calculerSortiesAujourdhui(data, entrepriseId);
            double soldeAujourdhui = caAujourdhui - sortiesAujourdhui;
            
            tresorerie.setCaAujourdhui(caAujourdhui);
            tresorerie.setSoldeAujourdhui(soldeAujourdhui);
        }

            return tresorerie;
    }

     // Récupère la liste paginée des dettes (factures impayées, ventes à crédit, dépenses en DETTE)
    
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<DetteItemDTO> getDettesDetaillees(HttpServletRequest request, int page, int size) {
        Long entrepriseId = validerEntrepriseEtPermissions(request);
        
        TresorerieData data = chargerDonnees(entrepriseId);

        java.util.List<DetteItemDTO> items = new java.util.ArrayList<>();

        for (FactureReelle facture : data.factures) {
            BigDecimal totalPaye = data.paiementsParFacture.getOrDefault(facture.getId(), BigDecimal.ZERO);
            double montantRestant = facture.getTotalFacture() - totalPaye.doubleValue();

            if (montantRestant > 0) {
                DetteItemDTO dto = new DetteItemDTO();
                dto.setId(facture.getId());
                dto.setType("FACTURE_IMPAYEE");
                // Montant de départ = total facture
                dto.setMontantInitial(facture.getTotalFacture());
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

        //  Dépenses générales avec source DETTE (filtrées par entreprise via chargerDonnees)
        List<DepenseGenerale> depensesDette = data.depensesGenerales.stream()
                .filter(d -> d.getSource() == SourceDepense.DETTE)
                .collect(Collectors.toList());

        for (DepenseGenerale depense : depensesDette) {
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

            items.add(dto);
        }

     
        List<EntreeGenerale> entreesDette = data.entreesGenerales.stream()
                .filter(e -> e.getSource() == SourceDepense.DETTE)
                .filter(e -> e.getDetteType() == null || !"PAIEMENT_FACTURE".equals(e.getDetteType()))
                .collect(Collectors.toList());

        for (EntreeGenerale entree : entreesDette) {
            DetteItemDTO dto = new DetteItemDTO();
            dto.setId(entree.getId());
            dto.setType("ENTREE_DETTE");
            Double montantInitial = getValeurDouble(entree.getMontant());
            Double montantRestant = entree.getMontantReste() != null
                    ? getValeurDouble(entree.getMontantReste())
                    : montantInitial;
            dto.setMontantInitial(montantInitial);
            dto.setMontantRestant(montantRestant);
            dto.setDate(entree.getDateCreation());
            dto.setDescription(entree.getDesignation());
            dto.setNumero(entree.getNumero());

            if (entree.getResponsable() != null) {
                dto.setResponsable(entree.getResponsable().getNomComplet());
                dto.setResponsableContact(entree.getResponsable().getPhone());
            }

            items.add(dto);
        }

      
        // Filtrer uniquement les ventes à crédit des caisses fermées
        // data.toutesVentes contient déjà uniquement les ventes des caisses fermées
        List<Vente> ventesCredit = data.toutesVentes.stream()
                .filter(v -> v.getModePaiement() == ModePaiement.CREDIT)
                .collect(Collectors.toList());
        
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
            Long venteEntrepriseId = v.getBoutique() != null && v.getBoutique().getEntreprise() != null 
                    ? v.getBoutique().getEntreprise().getId() : null;
            if (venteEntrepriseId != null) {
                factureVenteRepository.findByVenteIdAndEntrepriseId(v.getId(), venteEntrepriseId)
                    .ifPresent(f -> dto.setNumero(f.getNumeroFacture()));
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

            // Détail des produits vendus (dettes POS)
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
                if (remiseGlobaleVal > 0) {
                    dto.setTypeRemise("GLOBALE");
                } else if (lignes.stream().anyMatch(l -> l.getRemise() != null && l.getRemise() > 0)) {
                    dto.setTypeRemise("PAR_LIGNE");
                    dto.setRemiseGlobale(0.0);
                } else {
                    dto.setTypeRemise(null);
                }
            } else {
                dto.setProduits(java.util.Collections.emptyList());
                dto.setTypeRemise(remiseGlobaleVal > 0 ? "GLOBALE" : null);
            }

            items.add(dto);
        }

        items.sort((a, b) -> {
            if (a.getDate() == null && b.getDate() == null) return 0;
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

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

  
     // Récupère uniquement les dettes issues du POS (ventes à crédit).
    
    @Transactional(readOnly = true)
    public PaginatedResponseDTO<DetteItemDTO> getDettesPos(HttpServletRequest request, int page, int size,
            String periode, LocalDate dateDebut, LocalDate dateFin,
            String sortBy, String sortDir, Long vendeurId, Long boutiqueId) {
        Long entrepriseId = validerEntrepriseEtPermissions(request);

        // Charger les données avec ou sans filtre de période
        TresorerieData data;
        if (periode != null && !periode.trim().isEmpty() && !"toutes".equalsIgnoreCase(periode.trim())) {
            PeriodeDates periodeDates = calculerDatesPeriode(periode, dateDebut, dateFin);
            data = chargerDonneesAvecPeriode(entrepriseId, periodeDates);
        } else {
            data = chargerDonnees(entrepriseId);
        }

        List<Vente> ventesCredit = data.toutesVentes.stream()
                .filter(v -> v.getModePaiement() == ModePaiement.CREDIT)
                .filter(v -> vendeurId == null || (v.getVendeur() != null && v.getVendeur().getId().equals(vendeurId)))
                .filter(v -> boutiqueId == null || (v.getBoutique() != null && v.getBoutique().getId().equals(boutiqueId)))
                .collect(Collectors.toList());

        java.util.List<DetteItemDTO> items = new java.util.ArrayList<>();
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
            Long venteEntrepriseId = v.getBoutique() != null && v.getBoutique().getEntreprise() != null
                    ? v.getBoutique().getEntreprise().getId() : null;
            if (venteEntrepriseId != null) {
                factureVenteRepository.findByVenteIdAndEntrepriseId(v.getId(), venteEntrepriseId)
                        .ifPresent(f -> dto.setNumero(f.getNumeroFacture()));
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

            // Détail des produits vendus (quel produit, quelle quantité, quel montant, remise)
            if (v.getProduits() != null && !v.getProduits().isEmpty()) {
                java.util.List<DetteItemDTO.LigneProduitDetteDTO> lignes = v.getProduits().stream()
                        .map(vp -> {
                            DetteItemDTO.LigneProduitDetteDTO ligne = new DetteItemDTO.LigneProduitDetteDTO();
                            ligne.setProduitId(vp.getProduit() != null ? vp.getProduit().getId() : null);
                            ligne.setNomProduit(vp.getProduit() != null ? vp.getProduit().getNom() : null);
                            ligne.setQuantite(vp.getQuantite());
                            ligne.setPrixUnitaire(vp.getPrixUnitaire());
                            double remiseLigne = vp.getRemise();
                            ligne.setRemise(remiseLigne);
                            ligne.setMontantLigne(vp.getMontantLigne());
                            return ligne;
                        })
                        .collect(Collectors.toList());
                dto.setProduits(lignes);
                // Type de remise : soit globale soit par ligne, pas les deux
                if (remiseGlobaleVal > 0) {
                    dto.setTypeRemise("GLOBALE");
                } else if (lignes.stream().anyMatch(l -> l.getRemise() != null && l.getRemise() > 0)) {
                    dto.setTypeRemise("PAR_LIGNE");
                    dto.setRemiseGlobale(0.0);
                } else {
                    dto.setTypeRemise(null);
                }
            } else {
                dto.setProduits(java.util.Collections.emptyList());
                dto.setTypeRemise(remiseGlobaleVal > 0 ? "GLOBALE" : null);
            }

            items.add(dto);
        }

        // Trier selon sortBy et sortDir
        String sb = sortBy != null ? sortBy.toLowerCase() : "date";
        String sd = sortDir != null ? sortDir : "desc";
        items.sort((a, b) -> {
            int comparison = 0;
            switch (sb) {
                case "vendeur":
                    String v1 = a.getVendeurNom() != null ? a.getVendeurNom() : "";
                    String v2 = b.getVendeurNom() != null ? b.getVendeurNom() : "";
                    comparison = v1.compareToIgnoreCase(v2);
                    if ("desc".equalsIgnoreCase(sd)) comparison = -comparison;
                    break;
                case "boutique":
                case "boutiquenom":
                    String b1 = a.getBoutiqueNom() != null ? a.getBoutiqueNom() : "";
                    String b2 = b.getBoutiqueNom() != null ? b.getBoutiqueNom() : "";
                    comparison = b1.compareToIgnoreCase(b2);
                    if ("desc".equalsIgnoreCase(sd)) comparison = -comparison;
                    break;
                case "montantrestant":
                case "montant":
                    comparison = Double.compare(a.getMontantRestant() != null ? a.getMontantRestant() : 0.0,
                            b.getMontantRestant() != null ? b.getMontantRestant() : 0.0);
                    if ("desc".equalsIgnoreCase(sd)) comparison = -comparison;
                    break;
                case "date":
                default:
                    if (a.getDate() == null && b.getDate() == null) comparison = 0;
                    else if (a.getDate() == null) comparison = 1;
                    else if (b.getDate() == null) comparison = -1;
                    else comparison = a.getDate().compareTo(b.getDate());
                    if ("desc".equalsIgnoreCase(sd)) comparison = -comparison;
                    break;
            }
            return comparison;
        });

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

        double montantTotalCaisse = calculerMontantTotalCaisses(data.caissesFermees);
        
        List<Long> caisseIdsFermees = data.caissesFermees.stream().map(Caisse::getId).collect(Collectors.toList());
        
        double entreesMouvementsVente = 0.0;
        double entreesMouvementsAjout = 0.0;
        if (!caisseIdsFermees.isEmpty()) {
            List<MouvementCaisse> mouvementsVente = mouvementCaisseRepository.findByCaisseIdInAndTypeMouvement(
                caisseIdsFermees, TypeMouvementCaisse.VENTE);
            //  Inclure ESPECES et MOBILE_MONEY (OrangeMoney) dans la caisse
            entreesMouvementsVente = mouvementsVente.stream()
                    .filter(m -> m.getModePaiement() == ModePaiement.ESPECES 
                            || m.getModePaiement() == ModePaiement.MOBILE_MONEY)
                    .mapToDouble(m -> getValeurDouble(m.getMontant()))
                    .sum();
            
            List<MouvementCaisse> mouvementsAjout = mouvementCaisseRepository.findByCaisseIdInAndTypeMouvement(
                caisseIdsFermees, TypeMouvementCaisse.AJOUT);
            //  Inclure ESPECES et MOBILE_MONEY (OrangeMoney) dans la caisse
            entreesMouvementsAjout = mouvementsAjout.stream()
                    .filter(m -> m.getModePaiement() == ModePaiement.ESPECES 
                            || m.getModePaiement() == ModePaiement.MOBILE_MONEY)
                    .mapToDouble(m -> getValeurDouble(m.getMontant()))
                    .sum();
        }
        
        
        double entreesPaiementsEspeces = calculerEntreesPaiementsFactures(data, ModePaiement.ESPECES, null);
        double entreesGeneralesCaisse = calculerEntreesGeneralesCaisse(data);
        

        double entrees = entreesMouvementsVente + entreesMouvementsAjout + entreesPaiementsEspeces 
                + entreesGeneralesCaisse;
        
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
     * Inclut : ventes d'aujourd'hui + paiements factures d'aujourd'hui + entrées générales d'aujourd'hui
     * Exclut : les transferts de fonds
     */
    private double calculerCAAujourdhui(TresorerieData data, Long entrepriseId) {
        double ca = 0.0;

        // Ventes d'aujourd'hui (toutes les ventes, pas seulement celles des caisses fermées)
        List<Vente> ventesAujourdhui = venteRepository.findAllByEntrepriseId(entrepriseId).stream()
                .filter(v -> estAujourdhui(v.getDateVente()))
                .filter(v -> v.getStatus() != VenteStatus.REMBOURSEE)
                .collect(Collectors.toList());

        for (Vente vente : ventesAujourdhui) {
            ca += getValeurDouble(vente.getMontantTotal());
        }

        // Paiements factures d'aujourd'hui
        List<Paiement> paiementsAujourdhui = paiementRepository.findByEntrepriseId(entrepriseId).stream()
                .filter(p -> estAujourdhui(p.getDatePaiement()))
                .collect(Collectors.toList());

        for (Paiement paiement : paiementsAujourdhui) {
            ca += getValeurDouble(paiement.getMontant());
        }

        // Entrées générales d'aujourd'hui (en excluant les transferts et les paiements de factures)
        for (EntreeGenerale entree : data.entreesGenerales) {
            if (estAujourdhui(entree.getDateCreation()) 
                    && !estEntreeDeTransfert(entree.getDesignation())
                    && (entree.getDetteType() == null || !"PAIEMENT_FACTURE".equals(entree.getDetteType()))) {
                ca += getValeurDouble(entree.getMontant());
            }
        }

        return ca;
    }

    /**
     * Calcule les sorties d'aujourd'hui.
     * Inclut : dépenses générales d'aujourd'hui + mouvements caisse DEPENSE d'aujourd'hui + retraits d'aujourd'hui
     * Exclut : les transferts de fonds
     */
    private double calculerSortiesAujourdhui(TresorerieData data, Long entrepriseId) {
        double sorties = 0.0;

        // Dépenses générales d'aujourd'hui (en excluant les transferts)
        for (DepenseGenerale depense : data.depensesGenerales) {
            if (estAujourdhui(depense.getDateCreation()) 
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
     * Inclut : ventes + paiements factures + entrées générales
     * Exclut : les transferts de fonds
     */
    private double calculerCAPeriode(TresorerieData data, Long entrepriseId, PeriodeDates periodeDates) {
        double ca = 0.0;

        // Ventes de la période (déjà filtrées dans chargerDonneesAvecPeriode)
        for (Vente vente : data.toutesVentes) {
            if (vente.getStatus() != VenteStatus.REMBOURSEE) {
                ca += getValeurDouble(vente.getMontantTotal());
            }
        }

        // Paiements factures de la période
        if (periodeDates.filtrerParPeriode) {
            List<Paiement> paiementsPeriode = paiementRepository.findByEntrepriseIdAndDatePaiementBetween(
                    entrepriseId, periodeDates.dateDebut, periodeDates.dateFin);
            for (Paiement paiement : paiementsPeriode) {
                ca += getValeurDouble(paiement.getMontant());
            }
        }

        // Entrées générales de la période (en excluant les transferts et les paiements de factures)
        for (EntreeGenerale entree : data.entreesGenerales) {
            if (!estEntreeDeTransfert(entree.getDesignation())
                    && (entree.getDetteType() == null || !"PAIEMENT_FACTURE".equals(entree.getDetteType()))) {
                ca += getValeurDouble(entree.getMontant());
            }
        }

        return ca;
    }

    /**
     * Calcule les sorties pour une période donnée.
     * Inclut : dépenses générales + mouvements caisse DEPENSE + retraits
     * Exclut : les transferts de fonds
     */
    private double calculerSortiesPeriode(TresorerieData data, Long entrepriseId, PeriodeDates periodeDates) {
        double sorties = 0.0;

        // Dépenses générales de la période (en excluant les transferts)
        for (DepenseGenerale depense : data.depensesGenerales) {
            if (!estDepenseDeTransfert(depense.getDesignation())) {
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
}
