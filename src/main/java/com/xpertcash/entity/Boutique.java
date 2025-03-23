package com.xpertcash.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Boutique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomBoutique;
    private String adresse;
    private String telephone;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    @ManyToOne
    @JoinColumn(name = "entreprise_id", nullable = false)
    @JsonBackReference("boutique-entreprise")
    private Entreprise entreprise;

    @OneToMany(mappedBy = "boutique", cascade = CascadeType.ALL)
    @JsonManagedReference("boutique-stock")
    private List<Stock> stocks = new ArrayList<>();

    @OneToMany(mappedBy = "boutique", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Produit> produits;

    public List<Produit> getProduits() {
        return produits;
    }

    public void setProduits(List<Produit> produits) {
        this.produits = produits;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

}
