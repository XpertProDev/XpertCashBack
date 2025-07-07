package com.xpertcash.entity.Module;

import java.math.BigDecimal;

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
    private String code;
    private String nom;
    private String description;
    private boolean actifParDefaut;
    private boolean payant;
    @Column(nullable = true)
    private BigDecimal prix;

  
     public AppModule(String code,String nom, boolean actifParDefaut, boolean payant, BigDecimal prix) {
        this.code = code;
        this.nom = nom;
        this.actifParDefaut = actifParDefaut;
        this.payant = payant;
        this.prix = prix;

    }
}
