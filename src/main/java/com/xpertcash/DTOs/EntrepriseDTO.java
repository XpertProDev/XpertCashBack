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
        private String siege;
        private String nina;
        private String nif;
        private String banque;


        // Constructeur personnalisé
    public EntrepriseDTO(String nomEntreprise, String adminNom, LocalDateTime createdAt, String adresse, 
          String logo, String siege, String nina, String nif, String banque) {
        this.nomEntreprise = nomEntreprise;
        this.adminNom = adminNom;
        this.createdAt = createdAt;
        this.adresse = adresse;
        this.logo = logo;
        this.siege = siege;
        this.nina = nina;
        this.nif = nif;
        this.banque = banque;

    }

   

}
