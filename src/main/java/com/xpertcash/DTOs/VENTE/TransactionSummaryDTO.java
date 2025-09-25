package com.xpertcash.DTOs.VENTE;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TransactionSummaryDTO {
    
    // Résumé global
    private Double totalEntrees;
    private Double totalSorties;
    private Double soldeNet;
    
    // Détail des entrées
    private Double totalVentes;
    private Double totalPaiementsFactures;
    private Double totalAjoutsCaisse;
    private Double totalVersementsComptables;
    
    // Détail des sorties
    private Double totalRemboursements;
    private Double totalDepenses;
    private Double totalRetraitsCaisse;
    private Double totalTransferts;
    
    // Période
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String periode;
    
    // Détail par transaction
    private List<TransactionDetailDTO> transactions;
    
    @Data
    public static class TransactionDetailDTO {
        private Long id;
        private String type; // VENTE, PAIEMENT, REMBOURSEMENT, DEPENSE, etc.
        private String description;
        private Double montant;
        private LocalDateTime date;
        private String boutique;
        private String utilisateur;
        private String modePaiement;
        private String statut;
    }
}
