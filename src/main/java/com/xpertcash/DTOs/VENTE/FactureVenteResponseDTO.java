package com.xpertcash.DTOs.VENTE;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FactureVenteResponseDTO {

    private Long id;
    private String numeroFacture;
    private LocalDateTime dateEmission;
    private Double montantTotal;
    private String clientNom;
    private String clientNumero;
    private String boutiqueNom;
    private List<ProduitFactureResponse> produits;
    private String statutRemboursement;

}
