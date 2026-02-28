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
    private String logo;
    private LocalDateTime derniereConnexion;
    private String pays;
    private String adminPhone;
    private String adminEmail;

    /** Quota maximum d'utilisateurs autorisés pour cette entreprise. */
    private int maxUtilisateurs;

    /** Nombre d'utilisateurs actuellement non bloqués par quota (actifs par rapport au quota). */
    private long nombreUtilisateursActifs;

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


