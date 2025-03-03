package com.xpertcash.DTOs.USER;

import com.xpertcash.DTOs.BoutiqueResponse;
import com.xpertcash.entity.RoleType;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class UserRequest {
    private String nomComplet;
    private String nomEntreprise;
    private String email;
    private RoleType roleType;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Le téléphone doit être au format valide")
    private String phone;

    private String pays;
    private String adresseEntreprise;
    private String logoEntreprise;
    private List<BoutiqueResponse> boutiques;
}
