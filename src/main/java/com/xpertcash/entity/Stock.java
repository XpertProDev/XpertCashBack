package com.xpertcash.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "produit_id", nullable = false)
    @JsonManagedReference
    private Produits produit;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false) 
    private CategoryProduit category;

    @ManyToOne
    private Magasin magasin;

     private int quantite;
     private LocalDate dateExpiration;
     private LocalDateTime dateAjout = LocalDateTime.now();  
}
