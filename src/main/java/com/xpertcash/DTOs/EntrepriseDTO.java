package com.xpertcash.DTOs;

import java.time.LocalDateTime;

import lombok.Data;

@Data

public class EntrepriseDTO {

        private String nomEntreprise;
        private String adminNom;
        private LocalDateTime createdAt;
        private String adresse;
        private String logo;


        // Constructeur personnalisé
    public EntrepriseDTO(String nomEntreprise, String adminNom, LocalDateTime createdAt, String adresse, String logo) {
        this.nomEntreprise = nomEntreprise;
        this.adminNom = adminNom;
        this.createdAt = createdAt;
        this.adresse = adresse;
        this.logo = logo;

    }

   

}
