package com.xpertcash.DTOs.Boutique;



import java.time.LocalDateTime;

import com.xpertcash.entity.Enum.TypeBoutique;

import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class BoutiqueResponseVendeur {
    private Long id;
    private String nomBoutique;
    private String adresse;
    private TypeBoutique typeBoutique;
    private LocalDateTime assignedAt;
}