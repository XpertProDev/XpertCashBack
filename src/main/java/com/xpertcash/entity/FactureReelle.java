package com.xpertcash.entity;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
    private double totalHT;
    private double remise;
    private boolean tva;
    private double totalFacture;

    @Enumerated(EnumType.STRING)
    private StatutPaiementFacture statutPaiement;
    
    
    
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


}
