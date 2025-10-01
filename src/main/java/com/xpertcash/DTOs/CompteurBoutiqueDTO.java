package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompteurBoutiqueDTO {
    private Long boutiqueId;
    private String nomBoutique;
    private long totalProduits;
    private long totalEnStock;
}
