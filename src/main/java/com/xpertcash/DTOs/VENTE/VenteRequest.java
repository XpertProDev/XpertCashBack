package com.xpertcash.DTOs.VENTE;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class VenteRequest {
    private Long boutiqueId;
    private Long vendeurId;
    private Map<Long, Integer> produitsQuantites;
    private String description;
    private String clientNom;
    private String clientNumero;
    private String modePaiement;
    private Map<Long, Double> remises = new HashMap<>();
    private Map<Long, Double> prixPersonnalises = new HashMap<>(); // Prix personnalisés par produit pour les produits sans prix

    private Long clientId;
    private Long entrepriseClientId;
    
    private Double remiseGlobale = 0.0;
    private Double montantVerse;
}