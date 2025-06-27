package com.xpertcash.DTOs.Module;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActivationDemande {
    private String nomModule;
    private String numeroCarte;
    private String cvc;
    private String dateExpiration;
    private String nomProprietaire;
    private String prenomProprietaire;
    private String adresse;
    private String ville;
}
