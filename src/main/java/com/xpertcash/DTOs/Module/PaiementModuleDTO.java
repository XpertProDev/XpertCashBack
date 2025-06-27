package com.xpertcash.DTOs.Module;

import java.math.BigDecimal;

import com.xpertcash.entity.Module.PaiementModule;

import lombok.Data;

@Data
public class PaiementModuleDTO {

      private Long id;
    private BigDecimal montant;
    private String devise;
    private String nomProprietaire;
    private String prenomProprietaire;
    private String adresse;
    private String ville;
    private String datePaiement;
    private String referenceTransaction;
    private String codeModule;
    private String nomModule;

        public PaiementModuleDTO(PaiementModule paiement) {
        this.id = paiement.getId();
        this.montant = paiement.getMontant();
        this.devise = paiement.getDevise();
        this.nomProprietaire = paiement.getNomProprietaire();
        this.prenomProprietaire = paiement.getPrenomProprietaire();
        this.adresse = paiement.getAdresse();
        this.ville = paiement.getVille();
        this.datePaiement = paiement.getDatePaiement().toString();
        this.referenceTransaction = paiement.getReferenceTransaction();
        
        if (paiement.getModule() != null) {
            this.codeModule = paiement.getModule().getCode();
            this.nomModule = paiement.getModule().getNom();
        }
    }

}
