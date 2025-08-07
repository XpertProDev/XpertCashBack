package com.xpertcash.DTOs.VENTE;

import lombok.Data;
import java.util.Map;

@Data
public class VenteRequest {
    private Long boutiqueId;
    private Long vendeurId;
    private Map<Long, Integer> produitsQuantites; // produitId -> quantite
    private String description;
    private String clientNom;
    private String clientNumero;
    private String modePaiement;
    // private Double montantPaye;
}