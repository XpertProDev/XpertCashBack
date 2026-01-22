package com.xpertcash.DTOs.VENTE;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopProduitVenduDTO {
    private Long produitId;
    private String nomProduit;
    private Long totalQuantite;
    private Double totalMontant;
}
