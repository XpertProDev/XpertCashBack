package com.xpertcash.DTOs.VENTE;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class VenteResponseDTO {
    private Long id;
    private Double montantTotal;
    private String typeClient;
    private Long clientId;
    private Long entrepriseClientId;
    private String clientNom;
    private String clientNumero;
    private LocalDateTime dateVente;
    private Long caisseId;   
}
