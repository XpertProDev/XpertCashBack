package com.xpertcash.DTOs;

import java.time.LocalDateTime;

import com.xpertcash.entity.Facture;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FactureDTO {
     private Long id;
     private String numeroFacture;
    private String type;
    private Integer quantite;
    private String description;
    private LocalDateTime dateFacture;
    private String nomUtilisateur;
    private String telephoneUtilisateur;
    private String nomProduit;
    private Double prixUnitair;
    private Double total;


    public FactureDTO(Facture facture) {
        this.id = facture.getId();
        this.numeroFacture = facture.getNumeroFacture();
        this.type = facture.getType();
        this.quantite = facture.getQuantite();
        this.description = facture.getDescription();
        this.dateFacture = facture.getDateFacture();

        if (facture.getUser() != null) {
            this.nomUtilisateur = facture.getUser().getNomComplet();
            this.telephoneUtilisateur = facture.getUser().getPhone();
        }

        if (facture.getProduit() != null) {
            this.nomProduit = facture.getProduit().getNom();
            this.prixUnitair = facture.getProduit().getPrixVente();
            this.total = this.quantite * this.prixUnitair;
        }
    }

}
