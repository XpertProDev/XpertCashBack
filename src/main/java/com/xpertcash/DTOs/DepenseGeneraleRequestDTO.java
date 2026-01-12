package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepenseGeneraleRequestDTO {
    private String designation;
    private Long categorieId;
    private String nouvelleCategorieNom;
    private Double prixUnitaire;
    private Integer quantite;
    private String source; // CAISSE, BANQUE, MOBILE_MONEY, DETTE
    private String ordonnateur; // MANAGER, COMPTABLE
    private String numeroCheque;
    private String typeCharge; // CHARGE_FIXE, CHARGE_VARIABLE
    private Long produitId; // Facultatif (pour compatibilité, mais sera remplacé par categorieLieeId)
    private Long categorieLieeId; // Facultatif - Catégorie liée (Categorie) pour CHARGE_VARIABLE (peut être PRODUIT ou COMPTABILITE)
    private Long fournisseurId; // Facultatif
    private String pieceJointe; // URL ou chemin du fichier (facultatif)
}

