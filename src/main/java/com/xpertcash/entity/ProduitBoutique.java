package com.xpertcash.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ProduitBoutique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "produit_id", nullable = false)
    private Produits produit;

    @ManyToOne
    @JoinColumn(name = "boutique_id", nullable = false)
    private Boutique boutique;

    @Column(nullable = false)
    private Integer quantite;
}
