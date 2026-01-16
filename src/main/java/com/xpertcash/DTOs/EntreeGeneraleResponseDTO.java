package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntreeGeneraleResponseDTO {
    private Long id;
    private String numero;
    private String designation;
    private Long categorieId;
    private String categorieNom;
    private String categorieDescription;
    private Double prixUnitaire;
    private Integer quantite;
    private Double montant;
    private String source;
    private String modeEntree;
    private String numeroModeEntree;
    private String pieceJointe;
    private Long entrepriseId;
    private String entrepriseNom;
    private Long creeParId;
    private String creeParNom;
    private String creeParEmail;
    private Long responsableId;
    private String responsableNom;
    private String responsableEmail;
    private LocalDateTime dateCreation;
    private String typeTransaction;
    private String origine; // COMPTABILITE, FACTURE, BOUTIQUE (nom de la boutique)
    
    //  Champs pour identifier un paiement de dette
    private Long detteId; // ID de la dette payée (vente_id pour VENTE_CREDIT, entree_generale_id pour ENTREE_DETTE)
    private String detteType; // Type de dette: VENTE_CREDIT ou ENTREE_DETTE
    private String detteNumero; // Numéro de référence de la dette (numéro facture ou numéro entrée)
}

