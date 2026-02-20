package com.xpertcash.DTOs;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StockListDTO {
    private Long id;
    private Long produitId;
    private String produitNom;
    private String codeGenerique;
    private Long boutiqueId;
    private String boutiqueNom;
    private Integer stockActuel;
    private Integer seuilAlert;
    private LocalDate datePreemption;
}
