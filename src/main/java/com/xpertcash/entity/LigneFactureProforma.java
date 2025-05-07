package com.xpertcash.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor

public class LigneFactureProforma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "facture_proforma_id", nullable = false)
    @JsonBackReference
    private FactureProForma factureProForma;

    @ManyToOne
    @JoinColumn(name = "produit_id", nullable = false)
    @JsonIgnoreProperties({"prixVente", "prixAchat", "quantite", "seuilAlert", "description", "codeGenerique", "codeBare", "photo", "enStock", "createdAt", "lastUpdated", "categorie", "uniteDeMesure", "stocks", "factureProduits"})
    private Produit produit;

    private Integer quantite;
    private Double prixUnitaire;
    private Double montantTotal;
    private String description;

    @PrePersist
    @PreUpdate
    public void calculerMontantTotal() {
        if (this.quantite != null && this.prixUnitaire != null) {
            this.montantTotal = this.quantite * this.prixUnitaire;
        } else {
            this.montantTotal = 0.0;
        }
    }

    // Sérialisation personnalisée pour le nom du produit
    @JsonProperty("nomProduit")
    public String getNomProduit() {
        return produit != null ? produit.getNom() : null;
    }

    @JsonProperty("quantite")
    public Integer getQuantite() {
        return quantite;
    }

    @JsonProperty("prixUnitaire")
    public Double getPrixUnitaire() {
        return prixUnitaire;
    }

    @JsonProperty("montantTotal")
    public Double getMontantTotal() {
        return montantTotal;
    }

}