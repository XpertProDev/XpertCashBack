package com.xpertcash.DTOs.VENTE;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RemboursementResponse {
    private LocalDateTime dateRemboursement;
    private String details;
    private Double montant;
}

