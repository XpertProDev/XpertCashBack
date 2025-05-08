package com.xpertcash.DTOs.USER;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xpertcash.DTOs.BoutiqueResponse;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.User;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)

public class UserRequest {
    private long id;
    private String nomComplet;
    private String nomEntreprise;
    private String siege;
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

    public UserRequest(User user) {
        this.id = user.getId();
        this.nomComplet = user.getNomComplet();
        //this.email = user.getEmail();
        this.phone = user.getPhone();
        this.roleType = user.getRole().getName();
    }
    
}
