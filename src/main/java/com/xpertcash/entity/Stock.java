package com.xpertcash.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private Produits produit;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false) 
    private CategoryProduit category;

     private int quantite;
     private LocalDate dateExpiration;
     private LocalDateTime dateAjout = LocalDateTime.now();  
}
