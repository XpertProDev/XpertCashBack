package com.xpertcash.DTOs.Module;

import lombok.Data;

@Data
public class ModuleDTO {

    private Long id;
    private String nom;
    private String code;
    private boolean payant;
    private boolean actif;

    public ModuleDTO(Long id, String nom, String code, boolean payant, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.code = code;
        this.payant = payant;
        this.actif = actif;
    }

    // Getters et setters
}

