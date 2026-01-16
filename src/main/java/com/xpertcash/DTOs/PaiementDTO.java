package com.xpertcash.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.xpertcash.entity.Paiement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaiementDTO {

    private Long id;
    private BigDecimal montant;
    private LocalDateTime datePaiement;
    private String modePaiement;
    private String encaissePar;
    private String typeTransaction;
    private String description;
    private String objet; // Description de la facture (facture.getDescription())
    private String numeroFacture; // Numéro de la facture réelle
    private String statut;
    private String boutique;
    private String origine; // COMPTABILITE, FACTURE, BOUTIQUE (nom de la boutique)
    private List<LigneFactureDTO> lignesFacture; // Lignes de facture (produits/articles de la facture)

    //  Constructeur de mapping depuis l'entité Paiement
    public PaiementDTO(Paiement paiement) {
        this.id = paiement.getId();
        this.montant = paiement.getMontant();
        this.datePaiement = paiement.getDatePaiement();
        this.modePaiement = paiement.getModePaiement();
        this.encaissePar = paiement.getEncaissePar() != null
            ? paiement.getEncaissePar().getNomComplet()
            : null;
        this.boutique = "N/A"; // Par défaut pour les paiements de factures
    }
}
