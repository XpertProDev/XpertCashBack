package com.xpertcash.entity;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;
    private Double prixVente;
    private Double prixAchat;
    private Integer quantite;
    private Integer seuilAlert;
    private String Description;
    @Column(unique = true, nullable = false)
    private String codeGenerique;
    private String codeBare;
    private String photo;
    private Boolean enStock = false;

    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;


    @ManyToOne 
    @JoinColumn(name = "boutique_id")
    @JsonBackReference
    private Boutique boutique; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id", nullable = true)
    private Categorie categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_id", nullable = true)
    private Unite uniteDeMesure;

 

}
