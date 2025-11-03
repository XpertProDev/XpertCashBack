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

        // Récupérer tous les paiements de factures
        List<FactureReelle> toutesFacturesReelles = factureReelleRepository.findByEntrepriseId(entrepriseId);
        
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

        // Calculer les factures émisées
        double totalFacturesEmises = toutesFacturesReelles.stream()
                .mapToDouble(FactureReelle::getTotalFacture)
                .sum();

        // Total chiffre d'affaires = ventes + paiements de factures
        double total = totalVentes + totalPaiementsFactures;

        return new ComptabiliteDTO.ChiffreAffairesDTO(
                total,
                ventesJour,
                ventesMois,
                ventesAnnee,
                totalVentes,
                totalFacturesEmises,
                totalPaiementsFactures
        );
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

        return new ComptabiliteDTO.FacturationDTO(
                toutesFactures.size(),
                montantTotalFactures,
                montantPaye,
                montantImpaye,
                facturesJour.size(),
                montantDuJour,
                facturesMois.size(),
                montantDuMois,
                facturesAnnee.size(),
                montantDeLAnnee
        );
    }

    /**
     *  statistiques des dépenses
     */
    private ComptabiliteDTO.DepensesDTO calculerDepenses(Long entrepriseId) {
        List<Boutique> boutiques = boutiqueRepository.findByEntrepriseId(entrepriseId);
        List<Long> boutiqueIds = boutiques.stream().map(Boutique::getId).collect(Collectors.toList());

        if (boutiqueIds.isEmpty()) {
            return new ComptabiliteDTO.DepensesDTO(0, 0.0, 0, 0.0, 0, 0.0, 0, 0.0);
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

        return new ComptabiliteDTO.DepensesDTO(
                toutesDepenses.size(),
                montantTotal,
                depensesJour.size(),
                montantDuJour,
                depensesMois.size(),
                montantDuMois,
                depensesAnnee.size(),
                montantDeLAnnee
        );
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
                        .findByCaisseIdInAndTypeMouvementAndDateMouvementBetween(
                                caisseIds, TypeMouvementCaisse.DEPENSE, null, null);
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

        // Total clients = clients normaux + entreprises clients
        int totalClients = tousClients.size() + tousEntreprisesClients.size();
        int clientsActifsTotal = clientsActifsIds.size() + entrepriseClientsActifsIds.size();

        return new ComptabiliteDTO.ClientsDTO(
                totalClients,
                clientsActifsTotal,
                montantTotalAchete,
                meilleursClients
        );
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
                        "CLIENT"
                )).ajouterAchat(montantNet);
            } else if (vente.getEntrepriseClient() != null) {
                Long entrepriseClientId = vente.getEntrepriseClient().getId();
                String key = "ENTREPRISE_CLIENT_" + entrepriseClientId;
                statsParClient.computeIfAbsent(key, k -> new ClientStats(
                        vente.getEntrepriseClient().getId(),
                        vente.getEntrepriseClient().getNom(),
                        vente.getEntrepriseClient().getEmail(),
                        vente.getEntrepriseClient().getTelephone(),
                        "ENTREPRISE_CLIENT"
                )).ajouterAchat(montantNet);
            }
        }

        // Trier par montant acheté décroissant et prendre le top 3
        List<ComptabiliteDTO.MeilleurClientDTO> top3 = statsParClient.values().stream()
                .sorted((a, b) -> Double.compare(b.montantAchete, a.montantAchete))
                .limit(3)
                .map(cs -> new ComptabiliteDTO.MeilleurClientDTO(
                        cs.id,
                        cs.nomComplet,
                        cs.email,
                        cs.telephone,
                        cs.montantAchete,
                        cs.nombreAchats,
                        cs.type
                ))
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
        double montantAchete = 0.0;
        int nombreAchats = 0;

        ClientStats(Long id, String nomComplet, String email, String telephone, String type) {
            this.id = id;
            this.nomComplet = nomComplet;
            this.email = email;
            this.telephone = telephone;
            this.type = type;
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

        return new ComptabiliteDTO.VendeursDTO(
                tousVendeurs.size(),
                vendeursActifsIds.size(),
                chiffreAffairesTotal,
                meilleursVendeurs
        );
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
                        vente.getVendeur().getEmail()
                )).ajouterVente(montantNet);
            }
        }

        // Trier par chiffre d'affaires décroissant et prendre le top 3
        List<ComptabiliteDTO.MeilleurVendeurDTO> top3 = statsParVendeur.values().stream()
                .sorted((a, b) -> Double.compare(b.chiffreAffaires, a.chiffreAffaires))
                .limit(3)
                .map(vs -> new ComptabiliteDTO.MeilleurVendeurDTO(
                        vs.id,
                        vs.nomComplet,
                        vs.email,
                        vs.chiffreAffaires,
                        vs.nombreVentes
                ))
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
        double chiffreAffaires = 0.0;
        int nombreVentes = 0;

        VendeurStats(Long id, String nomComplet, String email) {
            this.id = id;
            this.nomComplet = nomComplet;
            this.email = email;
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

