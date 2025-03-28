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
    private long id;
    private String nomComplet;
    private String nomEntreprise;
    private String email;
    private RoleType roleType;
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Le téléphone doit être au format valide")
    private String phone;

    private String pays;
    private String adresseEntreprise;
    private String logoEntreprise;
    private Long entrepriseId;
    private List<BoutiqueResponse> boutiques;
    private String personalCode;


    public UserRequest(Long id, String nomComplet, String nomEntreprise, String email, RoleType roleType, String phone, String pays, String personalCode, String adresse, String logo, Long entrepriseId, List<BoutiqueResponse> boutiqueResponses) {
        this.id = id;
        this.nomComplet = nomComplet;
        this.nomEntreprise = nomEntreprise;
        this.email = email;
        this.roleType = roleType;
        this.phone = phone;
        this.pays = pays;
        this.personalCode = personalCode;
        this.entrepriseId = entrepriseId;
    }
}
