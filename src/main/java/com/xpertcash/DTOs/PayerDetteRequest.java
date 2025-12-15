package com.xpertcash.DTOs;

import lombok.Data;

@Data
public class PayerDetteRequest {
    private Long id;
    private String type;
    private Double montant;
    private String modePaiement;
}


