package com.xpertcash.DTOs;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AjouterStockRequest {
    private Map<Long, Integer> produitsQuantites;
    private String description;
    private Long boutiqueId;
    private String codeFournisseur;
    private Long fournisseurId;

}
