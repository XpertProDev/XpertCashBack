package com.xpertcash.DTOs;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class DepenseGeneraleResponse {
    private Long id;
    private Double montant;
    private String motif;
    private String modePaiement;
    private LocalDateTime dateDepense;
    private Long creeParId;
    private String creeParNom;
    private String creeParEmail;
    private Long entrepriseId;
    private String entrepriseNom;
}

