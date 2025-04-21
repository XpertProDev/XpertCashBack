package com.xpertcash.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    private String produitNom;
    private int quantite;
    private double prixUnitaire;
    private double montantTotal;

    // ✅ Constructeur depuis l'entité LigneFactureReelle
    public LigneFactureDTO(LigneFactureReelle ligneFacture) {
        this.id = ligneFacture.getId();
        this.produitNom = ligneFacture.getProduit() != null ? ligneFacture.getProduit().getNom() : null;
        this.quantite = ligneFacture.getQuantite();
        this.prixUnitaire = ligneFacture.getPrixUnitaire();
        this.montantTotal = ligneFacture.getMontantTotal();
    }
}

