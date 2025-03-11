package com.xpertcash.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;


@Entity
@Data

public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descriptionAjout;
    private String descriptionRetire;

    private Integer stockActuel = 0;
    private Integer quantiteAjoute = 0;
    private Integer quantiteRetirer = 0;
    private Integer stockApres = 0;

    private Integer seuilAlert;

    @ManyToOne
    @JoinColumn(name = "produit_id")
    @JsonBackReference("produit-stock")
    private Produit produit;

    @ManyToOne
    @JoinColumn(name = "boutique_id")
    @JsonBackReference("boutique-stock")
    private Boutique boutique;

    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    // Ajout d'un getter pour exposer l'id du produit dans le JSON
    @JsonProperty("produitId")
    public Long getProduitId() {
        return (produit != null) ? produit.getId() : null;
    }

    

}