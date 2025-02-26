package com.xpertcash.DTOs;

import jakarta.persistence.Entity;
import lombok.Data;

@Data
public class TransfertProduitDTO {
    private Long magasinId;
    private Long boutiqueId;
    private Long produitId;
    private int quantite;

}

