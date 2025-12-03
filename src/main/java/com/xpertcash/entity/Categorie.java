package com.xpertcash.entity;

import java.time.LocalDateTime;
import java.util.List;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Categorie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    @Column(length = 1000)
    private String description;
    private long produitCount;

    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "categorie")
    private List<Produit> produits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @Column(name = "origine_creation", length = 50)
    private String origineCreation; // "PRODUIT" ou "COMPTABILITE"
}
