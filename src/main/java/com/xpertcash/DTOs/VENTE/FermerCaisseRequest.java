package com.xpertcash.DTOs.VENTE;
import lombok.Data;

@Data
public class FermerCaisseRequest {
    private Long caisseId;
    private Long boutiqueId;
}