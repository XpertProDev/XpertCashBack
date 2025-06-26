package com.xpertcash.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code; // nom technique unique (ex: "CLIENT", "FACTURE", "FACTURE_REELLE")
    private String nom;
    private boolean actifParDefaut;
    private boolean payant;

  

     public AppModule(String code,String nom, boolean actifParDefaut, boolean payant) {
        this.code = code;
        this.nom = nom;
        this.actifParDefaut = actifParDefaut;
        this.payant = payant;
    }

    // Getters and Setters
}
