package com.xpertcash.DTOs.PROSPECT;

import com.xpertcash.entity.Enum.PROSPECT.ProspectType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProspectRequestDTO {
    private ProspectType type;
    
    // Champs pour ENTREPRISE
    private String nom;
    private String secteur;
    
    // Champs pour PARTICULIER
    private String nomComplet;
    private String profession;
    
    // Champs communs
    private String adresse;
    private String ville;
    private String pays;
    private String email;
    private String telephone;
    private String notes;
}
