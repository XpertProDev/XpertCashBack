package com.xpertcash.DTOs;

import java.time.LocalDateTime;

import lombok.Data;

@Data

public class EntrepriseDTO {

        private String nom;
        private String adminNom;
        private LocalDateTime createdAt;
        private String adresse;
        private String logo;
        private String siege;
        private String nina;
        private String nif;
        private String banque;
        private String email;
        private String telephone;
        private String pays;
        private String secteur;
        private String rccm;
        private String siteWeb;

        public EntrepriseDTO() {
        }

        // Constructeur personnalisé
    public EntrepriseDTO(String nom, String adminNom, LocalDateTime createdAt, String adresse, 
          String logo, String siege, String nina, String nif, String banque, String email, String telephone, String pays,
          String secteur, String rccm, String siteWeb) {
        this.nom = nom;
        this.adminNom = adminNom;
        this.createdAt = createdAt;
        this.adresse = adresse;
        this.logo = logo;
        this.siege = siege;
        this.nina = nina;
        this.nif = nif;
        this.banque = banque;
        this.email = email;
        this.telephone = telephone;
        this.pays = pays;
        this.secteur = secteur;
        this.rccm = rccm;
        this.siteWeb = siteWeb;

    }

   

}
