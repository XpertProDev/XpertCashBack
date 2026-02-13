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
    private String objet;
    private String numeroFacture;
    private Long factureId;
    private String statut;
    private String boutique;
    private String origine;
    private List<LigneFactureDTO> lignesFacture;
    
    // Informations de remise et TVA
    private Double remise;
    private Double tauxRemise;
    private Boolean tva;
    private Double totalHT;
    private Double totalTTC;
    
    // Informations client (facturation)
    private String clientNom;
    private String clientContact;

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
