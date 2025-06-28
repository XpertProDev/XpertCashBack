package com.xpertcash.DTOs.Module;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ModuleDTO {

    private Long id;
    private String nom;
    private String code;
    private String description;
    private boolean payant;
    private boolean actif;
    private BigDecimal prix;


    public ModuleDTO(Long id, String nom, String code,String description, boolean payant, boolean actif, BigDecimal prix) {
        this.id = id;
        this.nom = nom;
        this.code = code;
        this.description = description;
        this.payant = payant;
        this.actif = actif;
        this.prix = prix;
    }

    // Getters et setters
}

