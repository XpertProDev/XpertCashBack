package com.xpertcash.DTOs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xpertcash.entity.LigneFactureProforma;
import com.xpertcash.entity.LigneFactureReelle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)

public class LigneFactureDTO {
    private Long id;
   private ProduitDTO produit;
    private String ligneDescription;
    private int quantite;
    private double prixUnitaire;
    private double montantTotal;
    private List<LigneFactureDTO> ligneFactureProforma;

    //  Constructeur depuis l'entité LigneFactureReelle
    public LigneFactureDTO(LigneFactureReelle ligneFacture) {
        this.id = ligneFacture.getId();
       this.produit = (ligneFacture.getProduit() != null) 
        ? new ProduitDTO(ligneFacture.getProduit()) 
        : null;

        this.ligneDescription = ligneFacture.getLigneDescription() != null ? ligneFacture.getLigneDescription() : null;
        this.quantite = ligneFacture.getQuantite();
        this.prixUnitaire = ligneFacture.getPrixUnitaire();
        this.montantTotal = ligneFacture.getMontantTotal();
    }

    //  Constructeur depuis l'entité LigneFactureProforma
public LigneFactureDTO(LigneFactureProforma ligneFacture) {
    this.id = ligneFacture.getId();
    this.produit = ligneFacture.getProduit() != null ? new ProduitDTO(ligneFacture.getProduit()) : null;
    this.ligneDescription = ligneFacture.getLigneDescription();
    this.quantite = ligneFacture.getQuantite();
    this.prixUnitaire = ligneFacture.getPrixUnitaire();
    this.montantTotal = ligneFacture.getMontantTotal();
}

public List<LigneFactureDTO> getLigneFactureProforma() {
        return ligneFactureProforma;
    }

    public void setLigneFactureProforma(List<LigneFactureDTO> ligneFactureProforma) {
        this.ligneFactureProforma = ligneFactureProforma;
    }

}

