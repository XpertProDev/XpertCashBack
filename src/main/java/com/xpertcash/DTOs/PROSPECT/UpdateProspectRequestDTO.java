package com.xpertcash.DTOs.PROSPECT;

import com.xpertcash.entity.Enum.PROSPECT.ProspectType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProspectRequestDTO {
    private ProspectType type;
    
    // Champs pour ENTREPRISE
    private String companyName;
    private String sector;
    private String address;
    private String city;
    private String country;
    
    // Champs pour PARTICULIER
    private String firstName;
    private String lastName;
    private String position;
    
    // Champs communs
    private String email;
    private String phone;
    private String notes;
}
