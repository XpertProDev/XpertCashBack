package com.xpertcash.DTOs;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xpertcash.DTOs.CLIENT.ClientDTO;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.Enum.StatutFactureProForma;
import com.xpertcash.entity.Enum.StatutPaiementFacture;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FactureReelleDTO {

    private Long id;
    
    private String numeroFacture;
    private String description;
    private LocalDate dateCreation;
    private LocalDateTime dateCreationPro;
    private String utilisateurCreateur;
    private String utilisateurValidateur;
    private Long factureProFormaId;

    private double totalFacture;
    private double remise;
    private Double tauxRemise;
    private boolean tva;

   private StatutFactureProForma statut;
    private List<LigneFactureDTO> ligneFactureProforma;

    


    private double totalHT;

    private StatutPaiementFacture statutPaiement;
    // private UserRequest utilisateur;
    private EntrepriseClientDTO entrepriseClient;
    private ClientDTO client;
    private BigDecimal montantRestant;
    private List<LigneFactureDTO> lignesFacture;
    private String nomClient;
    private String nomEntrepriseClient; 

    //  Constructeur qui transforme une FactureReelle en FactureReelleDTO
    public FactureReelleDTO(FactureReelle facture, BigDecimal montantRestant) {
        this.id = facture.getId();
        this.numeroFacture = facture.getNumeroFacture();
        this.description = facture.getDescription();
        this.dateCreation = facture.getDateCreation();
        this.dateCreationPro = facture.getDateCreationPro();
        this.utilisateurCreateur = (facture.getUtilisateurCreateur() != null) ? facture.getUtilisateurCreateur().getNomComplet() : null;
        this.utilisateurValidateur = (facture.getUtilisateurValidateur() != null) ? facture.getUtilisateurValidateur().getNomComplet() : null;
        this.totalFacture = facture.getTotalFacture();
        this.remise = Math.round(facture.getRemise() * 100.0) / 100.0;
        this.tauxRemise = facture.getTauxRemise() != null ? Math.round(facture.getTauxRemise() * 100.0) / 100.0 : null;
        this.tva = facture.isTva();
        this.statutPaiement = facture.getStatutPaiement();
        this.entrepriseClient = (facture.getEntrepriseClient() != null) ? new EntrepriseClientDTO(facture.getEntrepriseClient()) : null;
        this.client = (facture.getClient() != null) ? new ClientDTO(facture.getClient()) : null;
        if (facture.getEntrepriseClient() != null) {
            this.nomEntrepriseClient = facture.getEntrepriseClient().getNom();
        }

        if (facture.getClient() != null) {
            this.nomClient = facture.getClient().getNomComplet();
        }
        /*this.utilisateur = (facture.getUtilisateurCreateur() != null) ? new UserRequest(facture.getUtilisateurCreateur()) : null;*/
        this.entrepriseClient = (facture.getEntrepriseClient() != null) ? new EntrepriseClientDTO(facture.getEntrepriseClient()) : null;
        this.client = (facture.getClient() != null) ? new ClientDTO(facture.getClient()) : null;
        this.montantRestant = montantRestant;
        this.lignesFacture = (facture.getLignesFacture() != null) ?
                facture.getLignesFacture().stream().map(LigneFactureDTO::new).collect(Collectors.toList()) : null;
        this.factureProFormaId = facture.getFactureProForma() != null
                ? facture.getFactureProForma().getId()
                : null;
        
        if (facture.getEntrepriseClient() != null) {
            this.nomEntrepriseClient = facture.getEntrepriseClient().getNom();
        }

        if (facture.getClient() != null) {
            this.nomClient = facture.getClient().getNomComplet();
        }

        
    }

    public List<LigneFactureDTO> getLigneFactureProforma() {
        return ligneFactureProforma;
    }

    public void setLigneFactureProforma(List<LigneFactureDTO> ligneFactureProforma) {
        this.ligneFactureProforma = ligneFactureProforma;
    }

    


}
