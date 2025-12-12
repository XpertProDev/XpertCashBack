package com.xpertcash.DTOs;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.xpertcash.entity.Paiement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaiementDTO {

    private BigDecimal montant;
    private LocalDateTime datePaiement;
    private String modePaiement;
    private String encaissePar;
    private String typeTransaction;

    // ✅ Constructeur de mapping depuis l'entité Paiement
    public PaiementDTO(Paiement paiement) {
        this.montant = paiement.getMontant();
        this.datePaiement = paiement.getDatePaiement();
        this.modePaiement = paiement.getModePaiement();
        this.encaissePar = paiement.getEncaissePar() != null
            ? paiement.getEncaissePar().getNomComplet()
            : null;
    }
}
