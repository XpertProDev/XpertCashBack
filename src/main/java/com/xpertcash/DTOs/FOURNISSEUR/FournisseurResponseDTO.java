package com.xpertcash.DTOs.FOURNISSEUR;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FournisseurResponseDTO {
    private Long id;
    private String nomComplet;
    private String nomSociete;
    private String description;
    private String pays;
    private String telephone;
    private String email;
    private String ville;
    private String adresse;
    private String photo;
    private Long entrepriseId;
    private String entrepriseNom;

    // constructeur, getters, setters
    
}
