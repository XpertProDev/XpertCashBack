package com.xpertcash.DTOs;

import lombok.Data;

@Data
public class RegistrationRequest {
    private String nomComplet;
    private String email;
    private String password;
    private String phone;
    /** Indicatif pays optionnel (ex: +223 pour le Mali). Si fourni, sera utilise pour formater le telephone. */
    private String indicatif;
    private String pays;
    private String nomEntreprise;
    private String nomBoutique;
}
