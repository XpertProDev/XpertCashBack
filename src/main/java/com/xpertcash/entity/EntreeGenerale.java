package com.xpertcash.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xpertcash.entity.Enum.SourceDepense;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "entree_generale")
public class EntreeGenerale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true, unique = false)
    private String numero;

    @Column(nullable = false)
    private String designation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Categorie categorie;

    @Column(nullable = false)
    private Double prixUnitaire;

    @Column(nullable = false)
    private Integer quantite;

    @Column(nullable = false)
    private Double montant;

    @Column(name = "montant_reste")
    private Double montantReste;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceDepense source;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_entree")
    private ModePaiement modeEntree;

    @Column(name = "numero_mode_entree")
    private String numeroModeEntree;

    @Column(name = "piece_jointe")
    private String pieceJointe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User creePar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User responsable;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    //  Champs pour lier les entrées de paiement aux dettes
    @Column(name = "dette_id")
    private Long detteId; // ID de la dette payée (vente_id pour VENTE_CREDIT, entree_generale_id pour ENTREE_DETTE)

    @Column(name = "dette_type", length = 50)
    private String detteType; // Type de dette: VENTE_CREDIT ou ENTREE_DETTE

    @Column(name = "dette_numero")
    private String detteNumero; // Numéro de référence de la dette (numéro facture ou numéro entrée)

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (montant == null && prixUnitaire != null && quantite != null) {
            montant = prixUnitaire * quantite;
        }
        // Initialiser montantReste pour les dettes
        if (source == SourceDepense.DETTE && montant != null && montantReste == null) {
            montantReste = montant;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (prixUnitaire != null && quantite != null) {
            montant = prixUnitaire * quantite;
        }
    }
}

