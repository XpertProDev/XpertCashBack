package com.xpertcash.service;

import com.xpertcash.DTOs.ActiviteHebdoDTO;
import com.xpertcash.DTOs.StatistiquesGlobalesDTO;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;
import com.xpertcash.entity.VENTE.Vente;
import com.xpertcash.entity.VENTE.VenteProduit;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.repository.VENTE.VenteRepository;
import com.xpertcash.repository.VENTE.VenteHistoriqueRepository;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatistiquesGlobalesService {

    @Autowired
    private ProduitRepository produitRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private VenteRepository venteRepository;

    @Autowired
    private VenteHistoriqueRepository venteHistoriqueRepository;

    @Autowired
    private FactureReelleRepository factureReelleRepository;

    @Autowired
    private FactureProformaRepository factureProformaRepository;

    @Autowired
    private AuthenticationHelper authHelper;

    /**
     * Récupère toutes les statistiques globales de l'entreprise
     */
    @Transactional(readOnly = true)
    public StatistiquesGlobalesDTO getStatistiquesGlobales(HttpServletRequest request) {
        // 🔐 Récupération de l'utilisateur connecté
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        if (user.getEntreprise() == null) {
            throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
        }

        Long entrepriseId = user.getEntreprise().getId();

        // Vérification des droits (seuls ADMIN et MANAGER peuvent voir les stats globales)
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à ces statistiques.");
        }

        // Récupération de toutes les statistiques
        StatistiquesGlobalesDTO stats = new StatistiquesGlobalesDTO();

        // 1. Statistiques des produits
        stats.setProduits(getProduitsStats(entrepriseId));

        // 2. Statistiques des ventes
        stats.setVentes(getVentesStats(entrepriseId));

        // 3. Statistiques des bénéfices
        stats.setBenefices(getBeneficesStats(entrepriseId));

        // 4. Statistiques des utilisateurs
        stats.setUtilisateurs(getUtilisateursStats(entrepriseId));

        return stats;
    }

    /**
     * Calcule les statistiques des produits pour une entreprise
     * - total : nombre de références de produits
     * - enStock : quantité totale en stock (somme des stockActuel)
     * - horsStock : quantité totale hors stock (produits avec stockActuel = 0)
     */
    private StatistiquesGlobalesDTO.ProduitsStats getProduitsStats(Long entrepriseId) {
        // Récupérer tous les produits actifs de l'entreprise
        List<Produit> produitsActifs = produitRepository.findAllByEntrepriseId(entrepriseId)
                .stream()
                .filter(p -> p.getDeleted() == null || !p.getDeleted())
                .collect(Collectors.toList());

        long total = produitsActifs.size();
        long quantiteEnStock = 0;
        long quantiteHorsStock = 0;

        // Calculer les quantités réelles en utilisant le Stock
        for (Produit produit : produitsActifs) {
            Stock stock = stockRepository.findByProduit(produit);
            
            if (stock != null && stock.getStockActuel() != null) {
                if (stock.getStockActuel() > 0) {
                    quantiteEnStock += stock.getStockActuel();
                } else {
                    // Stock à 0, on compte juste qu'il existe mais est vide
                    quantiteHorsStock += 0; // ou on pourrait compter le produit comme 1 unité hors stock
                }
            } else {
                // Pas de stock trouvé, on considère le produit comme hors stock
                // On pourrait incrémenter horsStock de 1 pour compter la référence
            }
        }

        return new StatistiquesGlobalesDTO.ProduitsStats(total, quantiteEnStock, quantiteHorsStock);
    }

    /**
     * Calcule les statistiques des ventes pour une entreprise
     */
    private StatistiquesGlobalesDTO.VentesStats getVentesStats(Long entrepriseId) {
        LocalDate today = LocalDate.now();

        // Ventes du jour
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        double ventesJour = calculerMontantVentes(entrepriseId, startOfDay, endOfDay);

        // Ventes du mois
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        double ventesMois = calculerMontantVentes(entrepriseId, startOfMonth, endOfMonth);

        // Ventes annuelles
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        LocalDate lastDayOfYear = today.withDayOfYear(today.lengthOfYear());
        LocalDateTime startOfYear = firstDayOfYear.atStartOfDay();
        LocalDateTime endOfYear = lastDayOfYear.atTime(LocalTime.MAX);
        double ventesAnnuel = calculerMontantVentes(entrepriseId, startOfYear, endOfYear);

        return new StatistiquesGlobalesDTO.VentesStats(ventesJour, ventesMois, ventesAnnuel);
    }

    /**
     * Calcule le montant total des ventes pour une période donnée
     */
    private double calculerMontantVentes(Long entrepriseId, LocalDateTime debut, LocalDateTime fin) {
        List<Vente> ventes = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
                entrepriseId, debut, fin
        );

        // Optimisation N+1 : Récupérer tous les remboursements d'un coup
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
     * Calcule les statistiques des bénéfices pour une entreprise
     */
    private StatistiquesGlobalesDTO.BeneficesStats getBeneficesStats(Long entrepriseId) {
        LocalDate today = LocalDate.now();

        // Bénéfices du jour
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        double beneficesJour = calculerBeneficeNet(entrepriseId, startOfDay, endOfDay);

        // Bénéfices du mois
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        double beneficesMois = calculerBeneficeNet(entrepriseId, startOfMonth, endOfMonth);

        // Bénéfices annuels
        LocalDate firstDayOfYear = today.withDayOfYear(1);
        LocalDate lastDayOfYear = today.withDayOfYear(today.lengthOfYear());
        LocalDateTime startOfYear = firstDayOfYear.atStartOfDay();
        LocalDateTime endOfYear = lastDayOfYear.atTime(LocalTime.MAX);
        double beneficesAnnuel = calculerBeneficeNet(entrepriseId, startOfYear, endOfYear);

        return new StatistiquesGlobalesDTO.BeneficesStats(beneficesJour, beneficesMois, beneficesAnnuel);
    }

    /**
     * Calcule le bénéfice net pour une période donnée
     */
    private double calculerBeneficeNet(Long entrepriseId, LocalDateTime debut, LocalDateTime fin) {
        List<Vente> ventes = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
                entrepriseId, debut, fin
        );

        // Optimisation N+1 : Récupérer tous les remboursements d'un coup
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

        double beneficeNet = 0.0;

        for (Vente vente : ventes) {
            double beneficeVente = 0.0;

            for (VenteProduit vp : vente.getProduits()) {
                double prixVente = vp.getMontantLigne();
                double prixAchat = (vp.getProduit().getPrixAchat() != null ? vp.getProduit().getPrixAchat() : 0.0) * vp.getQuantite();
                beneficeVente += prixVente - prixAchat;
            }

            // Déduire les remboursements
            double remboursements = remboursementsMap.getOrDefault(vente.getId(), 0.0);
            beneficeVente -= remboursements;
            beneficeNet += beneficeVente;
        }

        return beneficeNet;
    }

    /**
     * Calcule les statistiques des utilisateurs pour une entreprise
     */
    private StatistiquesGlobalesDTO.UtilisateursStats getUtilisateursStats(Long entrepriseId) {
        // Récupérer tous les utilisateurs de l'entreprise
        List<User> users = usersRepository.findByEntrepriseId(entrepriseId);
        
        long total = users.size();
        
        // Compter uniquement les vendeurs (rôle VENDEUR)
        long vendeurs = users.stream()
                .filter(u -> u.getRole() != null && u.getRole().getName() == RoleType.VENDEUR)
                .count();

        return new StatistiquesGlobalesDTO.UtilisateursStats(total, vendeurs);
    }

    /**
     * Récupère les statistiques d'activité hebdomadaire (7 derniers jours)
     */
    @Transactional(readOnly = true)
    public ActiviteHebdoDTO getActiviteHebdomadaire(HttpServletRequest request) {
        // 🔐 Récupération de l'utilisateur connecté
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        if (user.getEntreprise() == null) {
            throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
        }

        Long entrepriseId = user.getEntreprise().getId();

        // Vérification des droits (seuls ADMIN et MANAGER peuvent voir les stats)
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à ces statistiques.");
        }

        // Préparer les listes pour les 7 derniers jours
        List<String> dates = new java.util.ArrayList<>();
        List<Double> ventes = new java.util.ArrayList<>();
        List<Double> facturesReelles = new java.util.ArrayList<>();
        List<Double> facturesProforma = new java.util.ArrayList<>();

        LocalDate today = LocalDate.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM");

        // Parcourir les 7 derniers jours (du plus ancien au plus récent)
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

            // Format de la date
            dates.add(date.format(formatter));

            // 1. Calculer les ventes du jour
            List<Vente> ventesJour = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
                    entrepriseId, startOfDay, endOfDay
            );
            
            double montantVentes = 0.0;
            if (!ventesJour.isEmpty()) {
                List<Long> venteIds = ventesJour.stream().map(Vente::getId).collect(Collectors.toList());
                Map<Long, Double> remboursementsMap = venteHistoriqueRepository.sumRemboursementsByVenteIds(venteIds)
                        .stream()
                        .collect(Collectors.toMap(
                                obj -> (Long) obj[0],
                                obj -> ((Number) obj[1]).doubleValue()
                        ));

                for (Vente vente : ventesJour) {
                    double montantVente = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;
                    double remboursements = remboursementsMap.getOrDefault(vente.getId(), 0.0);
                    montantVentes += montantVente - remboursements;
                }
            }
            ventes.add(montantVentes);

            // 2. Calculer les factures réelles du jour
            List<FactureReelle> facturesReellesJour = factureReelleRepository.findByEntrepriseId(entrepriseId)
                    .stream()
                    .filter(f -> f.getDateCreation() != null && f.getDateCreation().equals(date))
                    .collect(Collectors.toList());
            
            double montantFacturesReelles = facturesReellesJour.stream()
                    .mapToDouble(FactureReelle::getTotalFacture)
                    .sum();
            facturesReelles.add(montantFacturesReelles);

            // 3. Calculer les factures proforma du jour
            List<FactureProForma> facturesProformaJour = factureProformaRepository.findByEntrepriseId(entrepriseId)
                    .stream()
                    .filter(f -> f.getDateCreation() != null && 
                                f.getDateCreation().toLocalDate().equals(date))
                    .collect(Collectors.toList());
            
            double montantFacturesProforma = facturesProformaJour.stream()
                    .mapToDouble(FactureProForma::getTotalFacture)
                    .sum();
            facturesProforma.add(montantFacturesProforma);
        }

        return new ActiviteHebdoDTO(dates, ventes, facturesReelles, facturesProforma);
    }
}

