package com.xpertcash.DTOs.VENTE;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class VersementComptableDTO {
    private Long id;
    private Double montantInitialCaisse;
    private Double montantCourantCaisse;
    private Double montantVerse;
    private LocalDateTime dateVersement;
    private String statut;
    private Long caisseId;
    private Long boutiqueId;
    private String nomBoutique;
    private String nomVendeur;
    private String nomComptable;
    private LocalDateTime dateValidation;

}
