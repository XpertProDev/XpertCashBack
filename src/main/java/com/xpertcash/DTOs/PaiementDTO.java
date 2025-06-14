package com.xpertcash.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.xpertcash.entity.Paiement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaiementDTO {

    private BigDecimal montant;
    private LocalDate datePaiement;
    private String modePaiement;
    private String encaissePar;

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
