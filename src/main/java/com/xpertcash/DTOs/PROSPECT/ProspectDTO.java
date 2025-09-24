package com.xpertcash.DTOs.PROSPECT;

import java.time.LocalDateTime;
import java.util.List;

import com.xpertcash.entity.Enum.PROSPECT.ProspectType;

public class ProspectDTO {
    public Long id;
    public ProspectType type;
    
    // Champs pour ENTREPRISE
    public String nom;
    public String sector;
    public String address;
    public String city;
    public String country;
    
    // Champs pour PARTICULIER
    public String nomComplet;
    public String adresse;
    public String pays;
    
    // Champs communs
    public String email;
    public String phone;
    public String notes;
    public LocalDateTime createdAt;
    public List<InteractionDTO> interactions;
}
