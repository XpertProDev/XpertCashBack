package com.xpertcash.service.VENTE;

import com.xpertcash.DTOs.VENTE.TransactionSummaryDTO;
import com.xpertcash.entity.*;
import com.xpertcash.entity.VENTE.*;
import com.xpertcash.repository.*;
import com.xpertcash.repository.VENTE.*;
import com.xpertcash.service.AuthenticationHelper;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.PermissionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class TransactionSummaryService {

    @Autowired
    private AuthenticationHelper authHelper;
    
    @Autowired
    private VenteRepository venteRepository;
    
    @Autowired
    private PaiementRepository paiementRepository;
    
    @Autowired
    private MouvementCaisseRepository mouvementCaisseRepository;
    
    @Autowired
    private VersementComptableRepository versementComptableRepository;
    
    @Autowired
    private VenteHistoriqueRepository venteHistoriqueRepository;
    
    @Autowired
    private TransfertRepository transfertRepository;
    
    
    @Autowired
    private FactureReelleRepository factureReelleRepository;

    /**
     * Récupère le résumé complet de toutes les transactions financières
     * pour l'entreprise de l'utilisateur connecté
     */
    @Transactional(readOnly = true)
    public TransactionSummaryDTO getTransactionSummary(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à ce résumé.");
        }
        
        Long entrepriseId = user.getEntreprise().getId();
        
        // Période par défaut : du début de l'année à maintenant
        LocalDate today = LocalDate.now();
        LocalDateTime startOfYear = today.withDayOfYear(1).atStartOfDay();
        LocalDateTime endOfYear = LocalDateTime.now();
        
        return getTransactionSummary(entrepriseId, startOfYear, endOfYear);
    }

    /**
     * Récupère le résumé des transactions pour une période donnée
     */
    @Transactional(readOnly = true)
    public TransactionSummaryDTO getTransactionSummary(Long entrepriseId, LocalDateTime dateDebut, LocalDateTime dateFin) {
        TransactionSummaryDTO summary = new TransactionSummaryDTO();
        summary.setDateDebut(dateDebut);
        summary.setDateFin(dateFin);
        summary.setPeriode("Du " + dateDebut.toLocalDate() + " au " + dateFin.toLocalDate());
        
        List<TransactionSummaryDTO.TransactionDetailDTO> allTransactions = new ArrayList<>();
        
        // 1. ENTRÉES - VENTES
        List<Vente> ventes = venteRepository.findByBoutique_Entreprise_IdAndDateVenteBetween(
            entrepriseId, dateDebut, dateFin);
        
        double totalVentes = 0.0;
        for (Vente vente : ventes) {
            double montantVente = vente.getMontantTotal() != null ? vente.getMontantTotal() : 0.0;
            totalVentes += montantVente;
            
            TransactionSummaryDTO.TransactionDetailDTO transaction = new TransactionSummaryDTO.TransactionDetailDTO();
            transaction.setId(vente.getId());
            transaction.setType("VENTE");
            transaction.setDescription("Vente - " + (vente.getClientNom() != null ? vente.getClientNom() : "Client passant"));
            transaction.setMontant(montantVente);
            transaction.setDate(vente.getDateVente());
            transaction.setBoutique(vente.getBoutique().getNomBoutique());
            transaction.setUtilisateur(vente.getVendeur().getNomComplet());
            transaction.setModePaiement(vente.getModePaiement() != null ? vente.getModePaiement().name() : null);
            transaction.setStatut(vente.getStatus() != null ? vente.getStatus().name() : null);
            allTransactions.add(transaction);
        }
        summary.setTotalVentes(totalVentes);
        
        // 2. ENTRÉES - PAIEMENTS DES FACTURES RÉELLES
        List<FactureReelle> factures = factureReelleRepository.findByEntrepriseIdAndDateCreationBetween(
            entrepriseId, dateDebut.toLocalDate(), dateFin.toLocalDate());
        
        double totalPaiementsFactures = 0.0;
        for (FactureReelle facture : factures) {
            List<Paiement> paiements = paiementRepository.findByFactureReelle(facture);
            for (Paiement paiement : paiements) {
                if (paiement.getDatePaiement().atStartOfDay().isAfter(dateDebut.minusDays(1)) && 
                    paiement.getDatePaiement().atStartOfDay().isBefore(dateFin.plusDays(1))) {
                    
                    double montantPaiement = paiement.getMontant().doubleValue();
                    totalPaiementsFactures += montantPaiement;
                    
                    TransactionSummaryDTO.TransactionDetailDTO transaction = new TransactionSummaryDTO.TransactionDetailDTO();
                    transaction.setId(paiement.getId());
                    transaction.setType("PAIEMENT_FACTURE");
                    
                    // Améliorer la description avec le statut de paiement
                    String statutFacture = facture.getStatutPaiement() != null ? facture.getStatutPaiement().name() : "INCONNU";
                    String description = "Paiement facture " + facture.getNumeroFacture();
                    
                    // Ajouter des détails sur le statut de paiement
                    if ("PARTIELLEMENT_PAYEE".equals(statutFacture)) {
                        double montantRestant = facture.getTotalFacture() - paiementRepository.sumMontantsByFactureReelle(facture.getId()).doubleValue();
                        description += " (Montant restant: " + montantRestant + ")";
                    }
                    
                    transaction.setDescription(description);
                    transaction.setMontant(montantPaiement);
                    transaction.setDate(paiement.getDatePaiement().atStartOfDay());
                    transaction.setBoutique("N/A");
                    transaction.setUtilisateur(paiement.getEncaissePar().getNomComplet());
                    transaction.setModePaiement(paiement.getModePaiement());
                    // Utiliser le statut réel de la facture au lieu de "PAYE" fixe
                    transaction.setStatut(statutFacture);
                    allTransactions.add(transaction);
                }
            }
        }
        summary.setTotalPaiementsFactures(totalPaiementsFactures);
        
        // 3. ENTRÉES - AJOUTS EN CAISSE
        List<MouvementCaisse> ajoutsCaisse = mouvementCaisseRepository.findByCaisse_Boutique_Entreprise_IdAndTypeMouvementAndDateMouvementBetween(
            entrepriseId, TypeMouvementCaisse.AJOUT, dateDebut, dateFin);
        
        double totalAjoutsCaisse = 0.0;
        for (MouvementCaisse mouvement : ajoutsCaisse) {
            totalAjoutsCaisse += mouvement.getMontant();
            
            TransactionSummaryDTO.TransactionDetailDTO transaction = new TransactionSummaryDTO.TransactionDetailDTO();
            transaction.setId(mouvement.getId());
            transaction.setType("AJOUT_CAISSE");
            transaction.setDescription(mouvement.getDescription());
            transaction.setMontant(mouvement.getMontant());
            transaction.setDate(mouvement.getDateMouvement());
            transaction.setBoutique(mouvement.getCaisse().getBoutique().getNomBoutique());
            transaction.setUtilisateur(mouvement.getCaisse().getVendeur().getNomComplet());
            transaction.setModePaiement(mouvement.getModePaiement() != null ? mouvement.getModePaiement().name() : null);
            transaction.setStatut("EFFECTUE");
            allTransactions.add(transaction);
        }
        summary.setTotalAjoutsCaisse(totalAjoutsCaisse);
        
        // 4. ENTRÉES - VERSEMENTS COMPTABLES VALIDÉS
        List<VersementComptable> versements = versementComptableRepository.findByCaisse_Boutique_Entreprise_IdAndStatutAndDateVersementBetween(
            entrepriseId, StatutVersement.VALIDE, dateDebut, dateFin);
        
        double totalVersementsComptables = 0.0;
        for (VersementComptable versement : versements) {
            totalVersementsComptables += versement.getMontant();
            
            TransactionSummaryDTO.TransactionDetailDTO transaction = new TransactionSummaryDTO.TransactionDetailDTO();
            transaction.setId(versement.getId());
            transaction.setType("VERSEMENT_COMPTABLE");
            transaction.setDescription("Versement comptable validé");
            transaction.setMontant(versement.getMontant());
            transaction.setDate(versement.getDateVersement());
            transaction.setBoutique(versement.getCaisse().getBoutique().getNomBoutique());
            transaction.setUtilisateur(versement.getCreePar().getNomComplet());
            transaction.setModePaiement("ESPECES");
            transaction.setStatut(versement.getStatut().name());
            allTransactions.add(transaction);
        }
        summary.setTotalVersementsComptables(totalVersementsComptables);
        
        // 5. SORTIES - REMBOURSEMENTS
        List<VenteHistorique> remboursements = venteHistoriqueRepository.findByVente_Boutique_Entreprise_IdAndActionAndDateActionBetween(
            entrepriseId, "REMBOURSEMENT_VENTE", dateDebut, dateFin);
        
        double totalRemboursements = 0.0;
        for (VenteHistorique remboursement : remboursements) {
            totalRemboursements += remboursement.getMontant();
            
            TransactionSummaryDTO.TransactionDetailDTO transaction = new TransactionSummaryDTO.TransactionDetailDTO();
            transaction.setId(remboursement.getId());
            transaction.setType("REMBOURSEMENT");
            transaction.setDescription(remboursement.getDetails());
            transaction.setMontant(remboursement.getMontant());
            transaction.setDate(remboursement.getDateAction());
            transaction.setBoutique(remboursement.getVente().getBoutique().getNomBoutique());
            transaction.setUtilisateur("N/A");
            transaction.setModePaiement("ESPECES");
            transaction.setStatut("EFFECTUE");
            allTransactions.add(transaction);
        }
        summary.setTotalRemboursements(totalRemboursements);
        
        // 6. SORTIES - DÉPENSES
        List<MouvementCaisse> depenses = mouvementCaisseRepository.findByCaisse_Boutique_Entreprise_IdAndTypeMouvementAndDateMouvementBetween(
            entrepriseId, TypeMouvementCaisse.DEPENSE, dateDebut, dateFin);
        
        double totalDepenses = 0.0;
        for (MouvementCaisse depense : depenses) {
            totalDepenses += depense.getMontant();
            
            TransactionSummaryDTO.TransactionDetailDTO transaction = new TransactionSummaryDTO.TransactionDetailDTO();
            transaction.setId(depense.getId());
            transaction.setType("DEPENSE");
            transaction.setDescription(depense.getDescription());
            transaction.setMontant(depense.getMontant());
            transaction.setDate(depense.getDateMouvement());
            transaction.setBoutique(depense.getCaisse().getBoutique().getNomBoutique());
            transaction.setUtilisateur(depense.getCaisse().getVendeur().getNomComplet());
            transaction.setModePaiement(depense.getModePaiement() != null ? depense.getModePaiement().name() : null);
            transaction.setStatut("EFFECTUE");
            allTransactions.add(transaction);
        }
        summary.setTotalDepenses(totalDepenses);
        
        // 7. SORTIES - RETRAITS DE CAISSE
        List<MouvementCaisse> retraits = mouvementCaisseRepository.findByCaisse_Boutique_Entreprise_IdAndTypeMouvementAndDateMouvementBetween(
            entrepriseId, TypeMouvementCaisse.RETRAIT, dateDebut, dateFin);
        
        double totalRetraitsCaisse = 0.0;
        for (MouvementCaisse retrait : retraits) {
            totalRetraitsCaisse += retrait.getMontant();
            
            TransactionSummaryDTO.TransactionDetailDTO transaction = new TransactionSummaryDTO.TransactionDetailDTO();
            transaction.setId(retrait.getId());
            transaction.setType("RETRAIT_CAISSE");
            transaction.setDescription(retrait.getDescription());
            transaction.setMontant(retrait.getMontant());
            transaction.setDate(retrait.getDateMouvement());
            transaction.setBoutique(retrait.getCaisse().getBoutique().getNomBoutique());
            transaction.setUtilisateur(retrait.getCaisse().getVendeur().getNomComplet());
            transaction.setModePaiement(retrait.getModePaiement() != null ? retrait.getModePaiement().name() : null);
            transaction.setStatut("EFFECTUE");
            allTransactions.add(transaction);
        }
        summary.setTotalRetraitsCaisse(totalRetraitsCaisse);
        
        // 8. SORTIES - TRANSFERTS ENTRE BOUTIQUES
        List<Transfert> transferts = transfertRepository.findByBoutiqueSource_Entreprise_IdOrBoutiqueDestination_Entreprise_IdAndDateTransfertBetween(
            entrepriseId, entrepriseId, dateDebut, dateFin);
        
        double totalTransferts = 0.0;
        for (Transfert transfert : transferts) {
            // Calculer la valeur du transfert basée sur le prix d'achat des produits
            double valeurTransfert = transfert.getQuantite() * transfert.getProduit().getPrixAchat();
            totalTransferts += valeurTransfert;
            
            TransactionSummaryDTO.TransactionDetailDTO transaction = new TransactionSummaryDTO.TransactionDetailDTO();
            transaction.setId(transfert.getId());
            transaction.setType("TRANSFERT");
            transaction.setDescription("Transfert " + transfert.getProduit().getNom() + 
                " de " + transfert.getBoutiqueSource().getNomBoutique() + 
                " vers " + transfert.getBoutiqueDestination().getNomBoutique());
            transaction.setMontant(valeurTransfert);
            transaction.setDate(transfert.getDateTransfert());
            transaction.setBoutique(transfert.getBoutiqueSource().getNomBoutique());
            transaction.setUtilisateur("SYSTEM");
            transaction.setModePaiement("N/A");
            transaction.setStatut("EFFECTUE");
            allTransactions.add(transaction);
        }
        summary.setTotalTransferts(totalTransferts);
        
        // Calculs des totaux
        double totalEntrees = totalVentes + totalPaiementsFactures + totalAjoutsCaisse + totalVersementsComptables;
        double totalSorties = totalRemboursements + totalDepenses + totalRetraitsCaisse + totalTransferts;
        double soldeNet = totalEntrees - totalSorties;
        
        summary.setTotalEntrees(totalEntrees);
        summary.setTotalSorties(totalSorties);
        summary.setSoldeNet(soldeNet);
        
        // Trier les transactions par date (plus récentes en premier)
        allTransactions.sort((t1, t2) -> t2.getDate().compareTo(t1.getDate()));
        summary.setTransactions(allTransactions);
        
        return summary;
    }

    /**
     * Récupère le résumé des transactions du jour
     */
    @Transactional(readOnly = true)
    public TransactionSummaryDTO getTransactionSummaryDuJour(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à ce résumé.");
        }
        
        Long entrepriseId = user.getEntreprise().getId();
        
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        
        return getTransactionSummary(entrepriseId, startOfDay, endOfDay);
    }

    /**
     * Récupère le résumé des transactions du mois
     */
    @Transactional(readOnly = true)
    public TransactionSummaryDTO getTransactionSummaryDuMois(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        // Vérification des droits
        RoleType role = user.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Vous n'avez pas les droits nécessaires pour accéder à ce résumé.");
        }
        
        Long entrepriseId = user.getEntreprise().getId();
        
        LocalDate today = LocalDate.now();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        
        return getTransactionSummary(entrepriseId, startOfMonth, endOfMonth);
    }
}
