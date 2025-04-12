package com.xpertcash.DTOs;

import com.xpertcash.entity.EntrepriseClient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntrepriseClientDTO {

    private Long id;
    private String nom;
    private String adresse;
    private String email;
    private String telephone;

      public EntrepriseClientDTO(EntrepriseClient entrepriseClient) {
        this.id = entrepriseClient.getId();
        this.nom = entrepriseClient.getNom();
        this.adresse = entrepriseClient.getAdresse();
        this.email = entrepriseClient.getEmail();
        this.telephone = entrepriseClient.getTelephone();
    }

}
