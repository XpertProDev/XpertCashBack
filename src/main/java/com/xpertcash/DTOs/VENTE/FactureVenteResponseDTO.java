package com.xpertcash.DTOs.VENTE;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FactureVenteResponseDTO {

    private Long id;
    private String numeroFacture;
    private LocalDateTime dateEmission;
    private Double montantTotal;
    private Double montantDette;
    private Double montantPaye; // Montant déjà payé (pour les ventes à crédit: montantTotalRembourse, sinon montantPaye)
    private String clientNom;
    private String clientNumero;
    private Long boutiqueId;
    private String boutiqueNom;
    private List<ProduitFactureResponse> produits;
    private String statutRemboursement;
    private Long caisseId;
    private String vendeur;
    private Double remiseGlobale; // Remise globale appliquée à la vente
    private String statutCaisse; // Statut de la caisse liée à la vente (OUVERTE, FERMEE, etc.)

}
