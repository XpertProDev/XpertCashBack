package com.xpertcash.DTOs;

import lombok.Data;

@Data
public class TransfertDTO {
    private Long id;
    private String produitNom;
    private String produitCodeGenerique;
    private String boutiqueSourceNom;
    private String boutiqueDestinationNom;
    private int quantite;
    private String dateTransfert;
}
