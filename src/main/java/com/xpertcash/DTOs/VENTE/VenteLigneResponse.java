package com.xpertcash.DTOs.VENTE;

import lombok.Data;

@Data
public class VenteLigneResponse {
    private Long produitId;
    private String nomProduit;
    private Integer quantite;
    private Double prixUnitaire;
    private Double montantLigne;
    private Double remise;
}