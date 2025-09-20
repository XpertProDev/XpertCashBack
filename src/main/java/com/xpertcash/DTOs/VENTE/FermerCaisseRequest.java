package com.xpertcash.DTOs.VENTE;

import lombok.Data;

@Data
public class FermerCaisseRequest {
    private Long boutiqueId;
    private Double montantEnMain; // Montant réel que le vendeur a en main
}