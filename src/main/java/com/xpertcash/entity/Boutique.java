package com.xpertcash.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Boutique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomBoutique;
    private String adresse;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    @ManyToOne
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @OneToMany(mappedBy = "boutique", cascade = CascadeType.ALL)
    private List<Stock> stocks = new ArrayList<>();

    @OneToMany(mappedBy = "boutique", fetch = FetchType.EAGER)
    private List<Produit> produits;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

}
