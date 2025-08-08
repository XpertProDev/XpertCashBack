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
    private LocalDateTime dateRelance;
    private boolean notifie;

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
    private List<LigneFactureDTO> ligneFactureProforma;
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
        this.dateCreationPro = facture.getDateCreation();

        
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

        // Initialisation de ClientDTO et EntrepriseClientDTO
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

        // Initialisation des noms de client et entreprise client
    this.nomClient = (facture.getClient() != null)
            ? facture.getClient().getNomComplet()
            : null;

    this.nomEntrepriseClient = (facture.getEntrepriseClient() != null)
            ? facture.getEntrepriseClient().getNom()
            : null;

    this.dateRelance = facture.getDateRelance();
    this.notifie = facture.isNotifie();
    }


      // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroFacture() {
        return numeroFacture;
    }

    public void setNumeroFacture(String numeroFacture) {
        this.numeroFacture = numeroFacture;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateCreationPro() {
        return dateCreationPro;
    }

    public void setDateCreationPro(LocalDateTime dateCreationPro) {
        this.dateCreationPro = dateCreationPro;
    }

    public String getUtilisateurCreateur() {
        return utilisateurCreateur;
    }

    public void setUtilisateurCreateur(String utilisateurCreateur) {
        this.utilisateurCreateur = utilisateurCreateur;
    }

    public String getUtilisateurModificateur() {
        return utilisateurModificateur;
    }

    public void setUtilisateurModificateur(String utilisateurModificateur) {
        this.utilisateurModificateur = utilisateurModificateur;
    }

    public String getNomEntreprise() {
        return nomEntreprise;
    }

    public void setNomEntreprise(String nomEntreprise) {
        this.nomEntreprise = nomEntreprise;
    }

    public Long getEntrepriseId() {
        return entrepriseId;
    }

    public void setEntrepriseId(Long entrepriseId) {
        this.entrepriseId = entrepriseId;
    }

    public double getTotalFacture() {
        return totalFacture;
    }

    public void setTotalFacture(double totalFacture) {
        this.totalFacture = totalFacture;
    }

    public double getTotalHT() {
        return totalHT;
    }

    public void setTotalHT(double totalHT) {
        this.totalHT = totalHT;
    }

    public double getRemise() {
        return remise;
    }

    public void setRemise(double remise) {
        this.remise = remise;
    }

    public Double getTauxRemise() {
        return tauxRemise;
    }

    public void setTauxRemise(Double tauxRemise) {
        this.tauxRemise = tauxRemise;
    }

    public boolean isTva() {
        return tva;
    }

    public void setTva(boolean tva) {
        this.tva = tva;
    }

    public StatutFactureProForma getStatut() {
        return statut;
    }

    public void setStatut(StatutFactureProForma statut) {
        this.statut = statut;
    }

    public List<LigneFactureDTO> getLigneFactureProforma() {
        return ligneFactureProforma;
    }

    public void setLigneFactureProforma(List<LigneFactureDTO> ligneFactureProforma) {
        this.ligneFactureProforma = ligneFactureProforma;
    }

    public StatutPaiementFacture getStatutPaiement() {
        return statutPaiement;
    }

    public void setStatutPaiement(StatutPaiementFacture statutPaiement) {
        this.statutPaiement = statutPaiement;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public ClientDTO getClient() {
        return client;
    }

    public void setClient(ClientDTO client) {
        this.client = client;
    }

    public EntrepriseClientDTO getEntrepriseClient() {
        return entrepriseClient;
    }

    public void setEntrepriseClient(EntrepriseClientDTO entrepriseClient) {
        this.entrepriseClient = entrepriseClient;
    }

    public List<LigneFactureDTO> getLignesFacture() {
        return lignesFacture;
    }

    public void setLignesFacture(List<LigneFactureDTO> lignesFacture) {
        this.lignesFacture = lignesFacture;
    }

    public String getNomClient() {
        return nomClient;
    }

    public void setNomClient(String nomClient) {
        this.nomClient = nomClient;
    }

    public String getNomEntrepriseClient() {
        return nomEntrepriseClient;
    }

    public void setNomEntrepriseClient(String nomEntrepriseClient) {
        this.nomEntrepriseClient = nomEntrepriseClient;
    }

    public LocalDateTime getDateRelance() {
        return dateRelance;
    }

    public void setDateRelance(LocalDateTime dateRelance) {
        this.dateRelance = dateRelance;
    }

    public boolean isNotifie() {
        return notifie;
    }

    public void setNotifie(boolean notifie) {
        this.notifie = notifie;
    }
}

