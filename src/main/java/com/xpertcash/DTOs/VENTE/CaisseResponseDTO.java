package com.xpertcash.DTOs.VENTE;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CaisseResponseDTO {
    private Long id;
    private Double montantInitial;
    private Double montantCourant;
    private String statut;
    private LocalDateTime dateOuverture;
    private LocalDateTime dateFermeture;
    private Long vendeurId;
    private String nomVendeur;
    private Long boutiqueId;
    private String nomBoutique;
}