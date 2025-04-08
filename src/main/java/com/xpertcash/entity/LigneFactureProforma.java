package com.xpertcash.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

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
    private Produit produit;

    private Integer quantite;
    private Double prixUnitaire;
    private Double montantTotal;
    private String description;

    @PrePersist
    @PreUpdate
    public void calculerMontantTotal() {
        this.montantTotal = this.quantite * this.prixUnitaire;
    }


}
