package com.xpertcash.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xpertcash.entity.Client;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)

public class ClientDTO {
    private Long id;
    private String nom;
    private String adresse;
    private String poste;
    private String email;
    private String pays;
    private String ville;
    private String telephone;
    private String photo;

    public ClientDTO(Client client) {
        this.id = client.getId();
        this.nom = client.getNomComplet();
        this.adresse = client.getAdresse();
        this.poste = client.getPoste();
        this.email = client.getEmail();
        this.pays = client.getPays();
        this.ville = client.getVille();
        this.photo = client.getPhoto();
        this.telephone = client.getTelephone();

      
    }

}
