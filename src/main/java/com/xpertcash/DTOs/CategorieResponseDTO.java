package com.xpertcash.DTOs;
import java.time.LocalDateTime;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xpertcash.DTOs.PRODUIT.ProduitDetailsResponseDTO;
import com.xpertcash.entity.Categorie;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
public class CategorieResponseDTO {
    private Long id;
    private String nom;
    private long produitCount;
    private LocalDateTime createdAt;
    private String origineCreation;
    private List<ProduitDetailsResponseDTO> produits;

    public CategorieResponseDTO(Categorie categorie) {
        this.id = categorie.getId();
        this.nom = categorie.getNom();
        this.produitCount = categorie.getProduitCount();
        this.createdAt = categorie.getCreatedAt();
        this.origineCreation = categorie.getOrigineCreation();
    }

    // Setter pour les produits en DTO
    public void setProduits(List<ProduitDetailsResponseDTO> produits) {
        this.produits = produits;
    }

}
