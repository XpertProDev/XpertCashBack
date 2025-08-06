package com.xpertcash.entity;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
public class VenteProduit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vente_id")
    private Vente vente;

    @ManyToOne
    @JoinColumn(name = "produit_id")
    private Produit produit;

    private Integer quantite;
    private Double prixUnitaire;
    private Double montantLigne;
}