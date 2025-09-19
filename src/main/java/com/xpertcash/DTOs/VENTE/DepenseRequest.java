package com.xpertcash.DTOs.VENTE;

import lombok.Data;

@Data
public class DepenseRequest {
    private Long boutiqueId;
    private Double montant;
    private String motif; 
}
