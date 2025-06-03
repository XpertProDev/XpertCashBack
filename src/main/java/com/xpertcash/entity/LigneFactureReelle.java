package com.xpertcash.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneFactureReelle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Produit produit;

    private int quantite;
    private double prixUnitaire;
    private String ligneDescription;
    private double montantTotal;
   

    @ManyToOne
    @JoinColumn(name = "facture_reelle_id")
    private FactureReelle factureReelle;

}