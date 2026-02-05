package com.xpertcash.DTOs.VENTE;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopVendeurDTO {
    private Long vendeurId;
    private String nomVendeur;
    private Long nombreVentes;
    private Double montantTotal;
}
