package com.xpertcash.entity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;



@Entity
@Data
public class CategoryProduit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  
    private Long id;

    @NotNull(message = "Le nom de la catégorie est obligatoire")
    @Column(nullable = false, unique = true)
    private String nomCategory;

    @ManyToOne
    @JoinColumn(name = "entreprise_id", nullable = false)  
    private Entreprise entreprise;

}


