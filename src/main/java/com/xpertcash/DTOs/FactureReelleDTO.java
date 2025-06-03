package com.xpertcash.DTOs;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xpertcash.DTOs.USER.UserRequest;
import com.xpertcash.entity.FactureReelle;
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
    private LocalDate dateCreation;
    private double totalFacture;
    private double remise;
    private Double tauxRemise;
    private boolean tva;
    private StatutPaiementFacture statutPaiement;
    private UserRequest utilisateur;
    private EntrepriseClientDTO entrepriseClient;
    private ClientDTO client;
    private List<LigneFactureDTO> lignesFacture;

       // ✅ Constructeur qui transforme une FactureReelle en FactureReelleDTO
    public FactureReelleDTO(FactureReelle facture) {
        this.id = facture.getId();
        this.numeroFacture = facture.getNumeroFacture();
        this.dateCreation = facture.getDateCreation();
        this.totalFacture = facture.getTotalFacture();
        this.remise = facture.getRemise();
        this.tauxRemise = facture.getTauxRemise();
        this.tva = facture.isTva();
        this.statutPaiement = facture.getStatutPaiement();
        this.utilisateur = (facture.getUtilisateurCreateur() != null) ? new UserRequest(facture.getUtilisateurCreateur()) : null;
        this.entrepriseClient = (facture.getEntrepriseClient() != null) ? new EntrepriseClientDTO(facture.getEntrepriseClient()) : null;
        this.client = (facture.getClient() != null) ? new ClientDTO(facture.getClient()) : null;
        this.lignesFacture = (facture.getLignesFacture() != null) ? 
            facture.getLignesFacture().stream().map(LigneFactureDTO::new).collect(Collectors.toList()) : null;
    }

    

}
