package com.xpertcash.DTOs.USER;

import com.xpertcash.entity.Enum.TypeBoutique;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBoutiqueDTO {
    private Long id;
    private String nomBoutique;
    private Boolean actif;
    private TypeBoutique typeBoutique;
    private Boolean isGestionnaireStock;
    private Boolean isGererProduits;
}
