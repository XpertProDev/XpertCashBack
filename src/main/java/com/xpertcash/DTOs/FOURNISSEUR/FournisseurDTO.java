package com.xpertcash.DTOs.FOURNISSEUR;
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
    private String description;
    private String pays;
    private String telephone; 
    private String email;
    private String ville;
    private String adresse;
    private LocalDateTime createdAt;
    private String photo;



     public FournisseurDTO(Fournisseur fournisseur) {
        this.nomComplet = fournisseur.getNomComplet();
        this.nomSociete = fournisseur.getNomSociete();
        this.description = fournisseur.getDescription();
        this.pays = fournisseur.getPays();
        this.telephone = fournisseur.getTelephone();
        this.email = fournisseur.getEmail();
        this.ville = fournisseur.getVille();
        this.adresse = fournisseur.getAdresse();
        this.createdAt = fournisseur.getCreatedAt();
        this.photo = fournisseur.getPhoto();


}

}
