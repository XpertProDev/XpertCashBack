package com.xpertcash.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Produits {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codeProduit;

    @NotNull(message = "Champs vide")
    @Column(nullable = false)
    private String nomProduit;

    @NotNull(message = "Champs vide")
    @Column(nullable = false)
    private String description;

    @NotNull(message = "Champs vide")
    @Column(nullable = false)
    private Double prix;

    @NotNull(message = "Champs vide")
    @Column(nullable = false)
    private Double prixAchat;

    @NotNull(message = "Champs vide")
    private int quantite;

    @ManyToOne
    @JoinColumn(name = "unite_mesure_id", nullable = false)
    private UniteMesure uniteMesure;


    

    @NotNull(message = "Champs vide")
    @Column(nullable = false)
    private int alertSeuil;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Utilisation de @JsonBackReference pour éviter des boucles infinies de sérialisation
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    @JsonProperty("categoryProduit")
    private CategoryProduit category;
}
