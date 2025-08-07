package com.xpertcash.DTOs;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xpertcash.DTOs.USER.UserRequest;
import com.xpertcash.entity.FactureProForma;
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
public class FactureProFormaDTO {

    private Long id;
    private String numeroFacture;
    private String description;
    private LocalDateTime dateCreation;
    private LocalDateTime dateCreationPro;
    private String utilisateurCreateur;
    private String utilisateurModificateur;
    private String nomEntreprise;
    private Long entrepriseId;
    private double totalFacture;
    private double totalHT;
    private double remise;
    private Double tauxRemise;
    private boolean tva;
    private StatutFactureProForma statut;
    private StatutPaiementFacture statutPaiement;
    private String justification;
    private ClientDTO client;
    private EntrepriseClientDTO entrepriseClient;
    private List<LigneFactureDTO> lignesFacture;
    private String nomClient;
    private String nomEntrepriseClient;


    public FactureProFormaDTO(FactureProForma facture) {
        
        this.id = facture.getId();
        this.numeroFacture = facture.getNumeroFacture();
        this.description = facture.getDescription();
        this.dateCreation = facture.getDateCreation();
        this.dateCreationPro = facture.getDateCreationPro();

        
        this.totalFacture = facture.getTotalFacture();
        this.totalHT = facture.getTotalHT();
        this.remise = facture.getRemise();
        this.tauxRemise = facture.getTauxRemise();
        this.tva = facture.isTva();
        this.statut = facture.getStatut();
        this.justification = facture.getJustification();

        this.utilisateurCreateur = (facture.getUtilisateurCreateur() != null)
                ? facture.getUtilisateurCreateur().getNomComplet()
                : null;

        this.utilisateurModificateur = (facture.getUtilisateurModificateur() != null)
                ? facture.getUtilisateurModificateur().getNomComplet()
                : null;

        this.entrepriseId = (facture.getEntreprise() != null)
                ? facture.getEntreprise().getId()
                : null;

        this.nomEntreprise = (facture.getEntreprise() != null)
                ? facture.getEntreprise().getNomEntreprise()
                : null;

        this.client = (facture.getClient() != null)
                ? new ClientDTO(facture.getClient())
                : null;

        this.entrepriseClient = (facture.getEntrepriseClient() != null)
                ? new EntrepriseClientDTO(facture.getEntrepriseClient())
                : null;

        this.lignesFacture = (facture.getLignesFacture() != null)
        ? facture.getLignesFacture().stream()
              .map(ligne -> new LigneFactureDTO(ligne))
              .collect(Collectors.toList())
        : null;


        this.nomClient = (facture.getClient() != null)
                ? facture.getClient().getNomComplet()
                : null;

        this.nomEntrepriseClient = (facture.getEntrepriseClient() != null)
                ? facture.getEntrepriseClient().getNom()
                : null;
    }
}

