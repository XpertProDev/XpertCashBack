package com.xpertcash.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private LocalDateTime dateApprobation;
     private LocalDateTime dateAnnulation;

     @Enumerated(EnumType.STRING)
     private MethodeEnvoi methodeEnvoi;

    
     // Montants calculés
     private double totalHT;  // Total avant taxes et remise
     private Double remise;   // Remise en montant
     private boolean tva;      // TVA 18% si applicable
     private double totalFacture; // Montant final à payer

     


    @ManyToOne
    @JsonIgnoreProperties({"personalCode", "phone", "photo", "createdAt", "activationCode", "activatedLien", "enabledLien", "lastActivity", "locked", "pays", "role"})
    private User utilisateurAnnulateur;


     

     


    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20)
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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entreprise_id")
    @JsonIgnoreProperties({"facturesProforma", "identifiantEntreprise", "utilisateurs", "adresse", "boutiques", "createdAt", "logo", "admin"})
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_createur_id")
    @JsonIgnoreProperties({"personalCode", "phone", "photo", "createdAt", "activationCode", "activatedLien", "enabledLien", "lastActivity", "locked", "pays", "role"})
    private User utilisateurCreateur;



    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_modificateur_id")
    @JsonIgnoreProperties({"personalCode", "phone", "photo", "createdAt", "activationCode", "activatedLien", "enabledLien", "lastActivity", "locked", "pays", "role"})
    private User utilisateurModificateur;
    
    
    @JsonProperty("nomProduit")
    public String getNomEntreprise() {
        return entreprise != null ? entreprise.getNomEntreprise() : null;
    }

    @JsonProperty("nomUtilisateurModificateur")
    public String getNomComplet() {
        return utilisateurModificateur != null ? utilisateurModificateur.getNomComplet() : null;
    }


    @JsonProperty("emailUtilisateurModificateur")
    public String getEmail() {
        return utilisateurModificateur != null ? utilisateurModificateur.getEmail() : null;
    }

    /*@JsonProperty("roleUtilisateurModificateur")
    public String getRole() {
        return utilisateurModificateur != null ? utilisateurModificateur.getRole().getName().name() : null;
    }*/
    

    
    

     private LocalDateTime dateRelance; 

     private LocalDateTime dernierRappelEnvoye;
     private boolean notifie = false; 
    




     @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_relanceur_id")
    @JsonIgnoreProperties({"personalCode", "phone", "photo", "createdAt", "activationCode", "activatedLien", "enabledLien", "lastActivity", "locked", "pays", "role"})
    private User utilisateurRelanceur;

    @ManyToOne
    @JoinColumn(name = "utilisateur_approbateur_id")
    private User utilisateurApprobateur;
  
    


    @ManyToMany
    @JoinTable(
        name = "facture_proforma_approbateurs",
        joinColumns = @JoinColumn(name = "facture_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> approbateurs;

     

    
}
