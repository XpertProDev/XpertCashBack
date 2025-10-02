package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlerteStockDTO {
    private Long produitId;
    private String nomProduit;
    private String codeGenerique;
    private Integer stockActuel;
    private Integer seuilAlert;
    private String nomBoutique;
    private Long boutiqueId;
}

