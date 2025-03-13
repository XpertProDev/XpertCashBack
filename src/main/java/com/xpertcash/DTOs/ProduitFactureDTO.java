package com.xpertcash.DTOs;

import com.xpertcash.entity.Produit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProduitFactureDTO {
    private String nomProduit;
    private Double prixUnitair;
    private Integer quantite;
    private Double total;
    private String codeGenerique;

    public ProduitFactureDTO(Produit produit, Integer quantite) {
        this.codeGenerique = produit.getCodeGenerique(); 
        this.nomProduit = produit.getNom();
        this.prixUnitair = produit.getPrixVente();
        this.quantite = quantite;
        this.total = this.prixUnitair * this.quantite;
    }
}
