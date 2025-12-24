package com.xpertcash.DTOs;

import lombok.Data;

@Data
public class TransfertFondsRequestDTO {
    private Double montant;
    private String source;
    private String destination;
    private String motif;
    private String personneALivrer;
    private String pieceJointe;
}

