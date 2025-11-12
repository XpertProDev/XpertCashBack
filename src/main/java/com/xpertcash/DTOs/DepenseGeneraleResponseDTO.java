package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepenseGeneraleResponseDTO {
    private Long id;
    private String numero;
    private String designation;
    private Long categorieId;
    private String categorieNom;
    private Double prixUnitaire;
    private Integer quantite;
    private Double montant;
    private String source;
    private String ordonnateur;
    private String numeroCheque;
    private String typeCharge;
    private Long produitId;
    private String produitNom;
    private Long fournisseurId;
    private String fournisseurNom;
    private String pieceJointe;
    private Long entrepriseId;
    private String entrepriseNom;
    private Long creeParId;
    private String creeParNom;
    private String creeParEmail;
    private LocalDateTime dateCreation;
}

