package com.xpertcash.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Data

public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer quantite;

    @ManyToOne
    @JoinColumn(name = "produit_id")
    private Produit produit; // Association au produit

    @ManyToOne
    @JoinColumn(name = "boutique_id")
    private Boutique boutique;  // Association à la boutique

    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

}