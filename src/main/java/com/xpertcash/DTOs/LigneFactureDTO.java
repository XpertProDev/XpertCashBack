package com.xpertcash.DTOs;

import com.xpertcash.entity.LigneFactureReelle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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

