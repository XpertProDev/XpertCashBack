package com.xpertcash.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class QuantitePourProduits {

    private Double valeur;
    private String unite;
    private String mesure;
    private String kilo;
}
