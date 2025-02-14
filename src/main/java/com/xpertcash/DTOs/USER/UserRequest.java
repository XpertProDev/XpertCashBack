package com.xpertcash.DTOs.USER;

import com.xpertcash.entity.RoleType;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserRequest {

    private String nomComplet;
    private String email;
    private RoleType roleType;
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Le téléphone doit être au format valide")
    private String phone;
    private String pays;
}
