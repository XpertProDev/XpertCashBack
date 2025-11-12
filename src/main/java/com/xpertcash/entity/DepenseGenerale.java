package com.xpertcash.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xpertcash.entity.Enum.Ordonnateur;
import com.xpertcash.entity.Enum.SourceDepense;
import com.xpertcash.entity.Enum.TypeCharge;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "depense_generale")
public class DepenseGenerale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, unique = false)
    private String numero;

    @Column(nullable = false)
    private String designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_depense_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private CategorieDepense categorie;

    @Column(nullable = false)
    private Double prixUnitaire;

    @Column(nullable = false)
    private Integer quantite;

    @Column(nullable = false)
    private Double montant; // Calculé automatiquement: prixUnitaire * quantite

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceDepense source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Ordonnateur ordonnateur;

    @Column(name = "numero_cheque")
    private String numeroCheque;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_charge", nullable = false)
    private TypeCharge typeCharge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Produit produit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Fournisseur fournisseur;

    @Column(name = "piece_jointe")
    private String pieceJointe; // URL ou chemin du fichier

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User creePar;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        // Calculer le montant automatiquement
        if (montant == null && prixUnitaire != null && quantite != null) {
            montant = prixUnitaire * quantite;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Recalculer le montant si prixUnitaire ou quantite change
        if (prixUnitaire != null && quantite != null) {
            montant = prixUnitaire * quantite;
        }
    }
}

