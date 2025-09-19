package com.xpertcash.DTOs.VENTE;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DepenseResponseDTO {
    private Long id;
    private Long caisseId;
    private Double montant;
    private String description;
    private LocalDateTime dateMouvement;
    private String nomVendeur;
    private String nomBoutique;
}
