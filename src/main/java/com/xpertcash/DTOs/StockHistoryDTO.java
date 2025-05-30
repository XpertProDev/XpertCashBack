package com.xpertcash.DTOs;

import java.time.LocalDateTime;

import com.xpertcash.entity.Enum.RoleType;

import lombok.Data;

@Data
public class StockHistoryDTO {

    private Long id;
    private String action;
    private Integer quantite;
    private Integer stockAvant;
    private Integer stockApres;
    private String description;
    private LocalDateTime createdAt;
    private String nomComplet;
    private String phone;
    private RoleType role;
    private String codeFournisseur;
    private String nomFournisseur;

}
