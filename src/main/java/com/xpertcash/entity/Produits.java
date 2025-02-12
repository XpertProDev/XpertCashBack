package com.xpertcash.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
public class Produits {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Champs vide")
    @Column(nullable = false)
    private String nomProduit;

    @NotNull(message = "Champs vide")
    @Column(nullable = false)
    private String description;

    @NotNull(message = "Champs vide")
    @Column(nullable = false)
    private Double prix;

    @Column(nullable = true)
    private String photo;

    @NotNull(message = "Champs vide")
    private String quantite;

    @NotNull(message = "Champs vide")
    @Column(nullable = false)
    private Integer seuil;

    @NotNull(message = "Champs vide")
    @Column(nullable = false)
    private String alertSeuil;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    //@JsonIgnore
    @JsonBackReference
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    @JsonProperty("categoryProduit")
    private CategoryProduit category;
}
