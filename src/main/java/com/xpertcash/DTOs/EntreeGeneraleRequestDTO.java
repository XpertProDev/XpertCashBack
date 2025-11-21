package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntreeGeneraleRequestDTO {
    private String designation;
    private Long categorieId;
    private String nouvelleCategorieNom;
    private Double prixUnitaire;
    private Integer quantite;
    private String source;
    private String modeEntree;
    private String numeroModeEntree;
    private Long responsableId;
    private String pieceJointe;
}

