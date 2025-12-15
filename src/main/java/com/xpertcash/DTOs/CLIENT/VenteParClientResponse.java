package com.xpertcash.DTOs.CLIENT;

import java.time.LocalDateTime;
import java.util.List;

import com.xpertcash.DTOs.VENTE.VenteLigneResponse;
import com.xpertcash.entity.ModePaiement;

import lombok.Data;

@Data
public class VenteParClientResponse {
    private Long venteId;
    private Long caisseId; 
    private Long boutiqueId;
    private Long vendeurId;
    private LocalDateTime dateVente;
    private Double montantTotal;
    private String description;
    private String nomVendeur;
    private String nomBoutique;
    private String clientNom;
    private String clientNumero;
    private ModePaiement modePaiement;
    private Double montantPaye;
    private Double montantRestant;
    private String statut;
    private List<VenteLigneResponse> lignes;
    private Double remiseGlobale;
    private String numeroFacture;
}

