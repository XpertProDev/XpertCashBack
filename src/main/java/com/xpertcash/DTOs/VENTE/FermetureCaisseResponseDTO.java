package com.xpertcash.DTOs.VENTE;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FermetureCaisseResponseDTO {
    private Long caisseId;
    private String numeroFermeture; // Numéro unique de la fermeture (ex: "FERM-001-12-2025")
    private LocalDateTime dateFermeture;
    private LocalDateTime dateOuverture;
    private Double montantTotal; // Montant total de toutes les ventes de cette fermeture
    private Integer nombreVentes; // Nombre de ventes dans cette fermeture
    private String nomBoutique;
    private Long boutiqueId;
    private String nomVendeur;
    private Long vendeurId;
    private Double montantInitial;
    private Double montantCourant;
    private Double montantEnMain;
    private Double ecart;
    private List<VenteResponse> ventes; // Liste des ventes (lignes de détails)
    private String typeTransaction;
    private String origine; // Nom de la boutique
}

