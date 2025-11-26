package com.xpertcash.DTOs;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SuperAdminEntrepriseStatsDTO {

    private Long entrepriseId;
    private String nomEntreprise;
    private boolean active;
    private LocalDateTime createdAt;

    private long nombreUtilisateurs;
    private long nombreBoutiques;
    private long nombreProduits;
    private long nombreStocks;

    private long nombreClientsTotal;
    private long nombreClientsParticuliers;
    private long nombreClientsEntreprises;

    private long nombreProspects;

    private long nombreFacturesProforma;
    private long nombreFacturesReelles;

    private long nombreCaissesOuvertes;
    private long nombreVentes;
}


