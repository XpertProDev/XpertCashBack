package com.xpertcash.DTOs.VENTE;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;


@Data
public class VenteResponse {
    private Long venteId;
   private CaisseDTO caisse;
    private Long boutiqueId;
    private Long vendeurId;
    private LocalDateTime dateVente;
    private Double montantTotal;
    private String description;
    private String nomVendeur;
    private String nomBoutique;
    private String clientNom;
    private String clientNumero;
    private String modePaiement;
    private Double montantPaye;
    private List<LigneVenteDTO> lignes;
    private Double remiseGlobale;
    private String numeroFacture;
    
    // Champs pour le suivi des remboursements
    private Double montantTotalRembourse;
    private LocalDateTime dateDernierRemboursement;
    private Integer nombreRemboursements;
    
    // Type de transaction
    private String typeTransaction;



    @Data
    public static class LigneVenteDTO {
        private Long produitId;
        private String nomProduit;
        private Integer quantite;
        private Double prixUnitaire;
        private Double montantLigne;
        private Double remise;
        
        // Champs pour le remboursement
        private Integer quantiteRemboursee;
        private Double montantRembourse;
        private boolean estRemboursee;

    }

     @Data
    public static class CaisseDTO {
        private Long id;
        private Double montantCourant;
        private String statut;
        private LocalDateTime dateOuverture;
        private LocalDateTime dateFermeture;
    }
}