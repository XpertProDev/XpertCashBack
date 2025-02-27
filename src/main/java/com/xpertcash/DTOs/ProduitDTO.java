package com.xpertcash.DTOs;

import lombok.Data;

@Data
public class ProduitDTO {
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
}
