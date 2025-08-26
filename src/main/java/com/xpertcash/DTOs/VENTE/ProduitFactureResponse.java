package com.xpertcash.DTOs.VENTE;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProduitFactureResponse {

    private Long produitId;
    private String nomProduit;
    private Integer quantite;
    private Double prixUnitaire;
    private Double remise;
    private Double montantLigne;

}
