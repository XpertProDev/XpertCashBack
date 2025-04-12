package com.xpertcash.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xpertcash.entity.Client;

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
    private String email;
    private String telephone;

    public ClientDTO(Client client) {
        this.id = client.getId();
        this.nom = client.getNomComplet();
        this.email = client.getEmail();
        this.telephone = client.getTelephone();

      
    }

}
