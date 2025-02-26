package com.xpertcash.DTOs;

import lombok.Data;

@Data
public class RegistrationRequest {
    private String nomComplet;
    private String email;
    private String password;
    private String phone;
    private String pays;
    private String nomEntreprise;
    private String nomBoutique;
}
