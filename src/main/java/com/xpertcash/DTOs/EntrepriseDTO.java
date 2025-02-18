package com.xpertcash.DTOs;

import java.time.LocalDateTime;

import lombok.Data;

@Data

public class EntrepriseDTO {

        private String nomEntreprise;
        private String adminNom;
        private LocalDateTime createdAt;


        // Constructeur personnalisé
    public EntrepriseDTO(String nomEntreprise, String adminNom, LocalDateTime createdAt) {
        this.nomEntreprise = nomEntreprise;
        this.adminNom = adminNom;
        this.createdAt = createdAt;
    }

   

}
