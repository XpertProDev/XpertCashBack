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
     private long produitCount;

    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "categorie")
    private List<Produit> produits;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise; 


}
