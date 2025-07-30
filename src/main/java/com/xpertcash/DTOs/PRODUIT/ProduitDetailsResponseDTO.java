package com.xpertcash.DTOs.PRODUIT;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
public class ProduitDetailsResponseDTO {
    private Long id;
    private String nom;
    private Double prixVente;
    private Double prixAchat;
    private Integer quantite;
    private Integer seuilAlert;
    private Long categorieId;
    private Long uniteId;
    private String codeBare;
    private String photo;
    private Boolean enStock;
    private String nomCategorie;
    private String nomUnite;
    private String typeProduit;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private LocalDate datePreemption;
    private Long boutiqueId;
    private String nomBoutique;

    // Constructeur si tu ne veux pas utiliser @AllArgsConstructor

}


