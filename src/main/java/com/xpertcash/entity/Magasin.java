package com.xpertcash.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Magasin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nomMagasin;

    @ManyToOne
    @JoinColumn(name = "entreprise_id", nullable = false)
    @JsonBackReference
    private Entreprise entreprise;


    //@JsonBackReference
    @OneToMany(mappedBy = "magasin", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Produits> produits = new ArrayList<>();



    public Magasin(String nomMagasin, Entreprise entreprise) {
        this.nomMagasin = nomMagasin;
        this.entreprise = entreprise;
    }

}
