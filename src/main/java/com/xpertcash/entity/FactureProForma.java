package com.xpertcash.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xpertcash.entity.Enum.StatutFactureProForma;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(indexes = {
    @Index(name = "idx_facture_proforma_entreprise_id", columnList = "entreprise_id"),
    @Index(name = "idx_facture_proforma_numero", columnList = "numero_facture"),
    @Index(name = "idx_facture_proforma_date_creation", columnList = "date_creation")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FactureProForma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroFacture;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;
    private LocalDate dateFacture;
    private String description;
    private LocalDateTime dateApprobation;
     private LocalDateTime dateAnnulation;

     @Enumerated(EnumType.STRING)
     private MethodeEnvoi methodeEnvoi;

    
     // Montants calculés
     private double totalHT;
     private Double remise;
     private Double tauxRemise;
     private boolean tva;
     private double totalFacture;

     private String noteModification;

     @Transient
    private String justification;

     @Transient
    private Long destinataireNoteId;

     


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
    @JsonBackReference
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_createur_id")
    @JsonIgnoreProperties({"personalCode", "phone", "photo", "createdAt", "activationCode", "activatedLien", "enabledLien", "lastActivity", "locked", "pays", "role", "qrCodeUrl"})
    private User utilisateurCreateur;

     @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_validateur_id")
    @JsonIgnoreProperties({"personalCode", "phone", "photo", "createdAt", "activationCode", "activatedLien", "enabledLien", "lastActivity", "locked", "pays", "role", "qrCodeUrl"})
    private User utilisateurValidateur;



    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_modificateur_id")
    @JsonIgnoreProperties({"personalCode", "phone", "photo", "createdAt", "activationCode", "activatedLien", "enabledLien", "lastActivity", "locked", "pays", "role", "qrCodeUrl"})
    private User utilisateurModificateur;
    
    
    @JsonProperty("nomEntreprise")
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