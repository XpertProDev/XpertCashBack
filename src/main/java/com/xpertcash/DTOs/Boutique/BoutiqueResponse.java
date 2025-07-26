package com.xpertcash.DTOs.Boutique;



import java.time.LocalDateTime;

import com.xpertcash.entity.Enum.TypeBoutique;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class BoutiqueResponse {
    private Long id;
    private String nomBoutique;
    private String adresse;
    private String telephone;
    private String email;
    private LocalDateTime createdAt;
    private boolean actif = true;
    private TypeBoutique typeBoutique;
    
    
}