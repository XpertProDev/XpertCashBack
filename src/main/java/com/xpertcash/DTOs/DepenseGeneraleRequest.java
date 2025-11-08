package com.xpertcash.DTOs;

import lombok.Data;

@Data
public class DepenseGeneraleRequest {
    private Double montant;
    private String motif;
    private String modePaiement;
}

