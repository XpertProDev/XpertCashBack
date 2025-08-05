package com.xpertcash.DTOs.VENTE;
import lombok.Data;

@Data
public class OuvrirCaisseRequest {
    private Long boutiqueId;
    private Double montantInitial;
}