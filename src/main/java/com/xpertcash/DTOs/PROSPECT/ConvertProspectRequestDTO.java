package com.xpertcash.DTOs.PROSPECT;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConvertProspectRequestDTO {
    private Long produitId; // ID du produit/service acheté (obligatoire)
    private Double montantAchat; // Montant de l'achat (optionnel, peut être récupéré depuis le produit)
    private String notesAchat; // Notes sur l'achat (optionnel)
}
