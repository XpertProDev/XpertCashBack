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
    private String secter;
    private String address;
    private String city;
    private String country;
    
    // Champs pour PARTICULIER
    private String nomComplet;
    private String adresse;
    private String pays;
    
    // Champs communs
    private String email;
    private String telephone;
    private String notes;
}
