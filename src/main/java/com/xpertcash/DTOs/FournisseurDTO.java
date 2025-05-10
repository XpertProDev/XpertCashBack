package com.xpertcash.DTOs;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xpertcash.entity.Fournisseur;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FournisseurDTO {

    private String nomComplet;
    private String nomSociete;
    private String adresse;
    private String pays;
    private String ville;
    private String telephone;
    private String email;
    private LocalDateTime createdAt;

     public FournisseurDTO(Fournisseur fournisseur) {
        this.nomComplet = fournisseur.getNomComplet();
        this.nomSociete = fournisseur.getNomSociete();
        this.adresse = fournisseur.getAdresse();
        this.pays = fournisseur.getPays();
        this.ville = fournisseur.getVille();
        this.telephone = fournisseur.getTelephone();
        this.email = fournisseur.getEmail();
        this.createdAt = fournisseur.getCreatedAt();

}

}
