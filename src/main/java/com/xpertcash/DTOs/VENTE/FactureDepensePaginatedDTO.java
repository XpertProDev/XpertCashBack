package com.xpertcash.DTOs.VENTE;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FactureDepensePaginatedDTO {
    
    // Informations de pagination
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
    
    // Résumé des totaux
    private Double totalFacturesVente;
    private Double totalDepenses;
    private Double soldeNet;
    private int nombreFacturesVente;
    private int nombreDepenses;
    
    // Liste des éléments
    private List<FactureDepenseItemDTO> items;
    
    @Data
    public static class FactureDepenseItemDTO {
        private Long id;
        private String type; // "FACTURE_VENTE" ou "DEPENSE"
        private String numeroFacture; // Pour les factures de vente
        private String description;
        private Double montant;
        private LocalDateTime date;
        private String boutique;
        private String vendeur;
        private String modePaiement;
        private String statut;
        
        // Informations spécifiques aux factures de vente
        private String clientNom;
        private String clientNumero;
        private Double remiseGlobale;
        private String numeroFactureVente;
        private Long caisseId;
        private List<VenteProduitDTO> produits;
        
        // Informations spécifiques aux dépenses
        private String motifDepense;
        private String typeMouvement; // AJOUT, RETRAIT, DEPENSE, etc.
        
        @Data
        public static class VenteProduitDTO {
            private Long produitId;
            private String nomProduit;
            private Integer quantite;
            private Double prixUnitaire;
            private Double montantLigne;
            private Double remise;
        }
    }
}
