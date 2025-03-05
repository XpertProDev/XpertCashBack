package com.xpertcash.DTOs;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xpertcash.entity.Categorie;
import com.xpertcash.entity.Unite;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProduitRequest {
    private String nom;
    private String description;
    private Double prixVente;
    private Double prixAchat;
    private Integer quantite;
    private Integer seuilAlert;
    private Long categorieId;      
    private Long uniteId;
    private String codeBare;
    private String photo;
    private Boolean enStock;
}

