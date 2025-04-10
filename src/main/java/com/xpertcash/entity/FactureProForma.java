package com.xpertcash.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactureProForma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroFacture;
    private LocalDate dateCreation;
    private String description;
    
     // Montants calculés
     private double totalHT;  // Total avant taxes et remise
     private Double remise;   // Remise en montant
     private boolean tva;      // TVA 18% si applicable
     private double totalFacture; // Montant final à payer

     @Enumerated(EnumType.STRING)
    private StatutPaiementFacture statutPaiement = StatutPaiementFacture.EN_ATTENTE;


    @Enumerated(EnumType.STRING)
    private StatutFactureProForma statut = StatutFactureProForma.BROUILLON;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id")
    private Client client;

     @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entrepriseClient_id")
     @JsonIgnoreProperties({"clients", "createdAt"})
    private EntrepriseClient entrepriseClient;

    @OneToMany(mappedBy = "factureProForma", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneFactureProforma> lignesFacture;

    // Relation avec l'Entreprise de l'Utilisateur
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entreprise_id")
    @JsonIgnoreProperties({"utilisateurs", "createdAt", "logo", "adresse"})
    private Entreprise entreprise;


     private LocalDateTime dateRelance; 

     private LocalDateTime dernierRappelEnvoye;
     private boolean notifie = false; 
    

   

    
}
