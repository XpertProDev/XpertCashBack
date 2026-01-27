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
    private Long ordonnateurId;
    private String ordonnateurNom;
    private String numeroCheque;
    private String typeCharge;
    private Long produitId;
    private String produitNom;
    private Long categorieLieeId; // Catégorie liée (Categorie) pour CHARGE_VARIABLE
    private String categorieLieeNom; // Nom de la catégorie liée
    private String categorieLieeOrigine; // Origine de la catégorie liée (PRODUIT ou COMPTABILITE)
    private Long fournisseurId;
    private String fournisseurNom;
    private String pieceJointe;
    private Long entrepriseId;
    private String entrepriseNom;
    private Long creeParId;
    private String creeParNom;
    private String creeParEmail;
    private LocalDateTime dateCreation;
    private String typeTransaction;
    private String origine; // COMPTABILITE, FACTURE, BOUTIQUE (nom de la boutique)
}

