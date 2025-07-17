package com.xpertcash.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xpertcash.entity.Enum.StatutFactureReelle;
import com.xpertcash.entity.Enum.StatutPaiementFacture;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactureReelle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroFacture;
    private String description;
    private LocalDate dateCreation;
   private LocalDateTime dateCreationPro;
    private double totalHT;
    private double remise;
    private Double tauxRemise;
    private boolean tva;
    private double totalFacture;
     private LocalDateTime dateAnnulation;



    @Enumerated(EnumType.STRING)
    private StatutPaiementFacture statutPaiement;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20)
    // Statut de la facture réelle par defaut ya pas de statut
    private StatutFactureReelle statut = StatutFactureReelle.VALIDE;

    



    @ManyToOne
    private User utilisateurCreateur;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entrepriseClient_id")
    @JsonIgnoreProperties({"clients", "createdAt"})
    private EntrepriseClient entrepriseClient;

    @OneToMany(mappedBy = "factureReelle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneFactureReelle> lignesFacture;

      @ManyToOne
    @JsonIgnoreProperties({"personalCode", "phone", "photo", "createdAt", "activationCode", "activatedLien", "enabledLien", "lastActivity", "locked", "pays", "role"})
    private User utilisateurAnnulateur;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entreprise_id")
    @JsonIgnoreProperties({"facturesProforma", "identifiantEntreprise", "utilisateurs", "adresse", "boutiques", "createdAt", "logo", "admin"})
    private Entreprise entreprise;

    @JsonProperty("nomProduit")
    public String getNomEntreprise() {
        return entreprise != null ? entreprise.getNomEntreprise() : null;
    }

    @ManyToOne
    @JoinColumn(name = "facture_proforma_id")
    @JsonIgnoreProperties({"facturesReelles"})
    private FactureProForma factureProForma;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_validateur_id")
    @JsonIgnoreProperties({"personalCode", "phone", "photo", "createdAt", "activationCode", "activatedLien", "enabledLien", "lastActivity", "locked", "pays", "role"})
    private User utilisateurValidateur;


    @OneToMany(mappedBy = "factureReelle", cascade = CascadeType.ALL)
    private List<Paiement> paiements = new ArrayList<>();

    public List<Paiement> getPaiements() {
        return paiements;
    }

    public void setPaiements(List<Paiement> paiements) {
        this.paiements = paiements;
    }



}