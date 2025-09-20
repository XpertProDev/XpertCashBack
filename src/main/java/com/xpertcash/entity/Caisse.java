package com.xpertcash.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.xpertcash.entity.VENTE.StatutCaisse;

@Data
@Entity
public class Caisse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double montantInitial;
    private Double montantCourant;
    private Double montantEnMain; // Montant réel saisi lors de la fermeture
    private Double ecart; // Différence entre montantCourant et montantEnMain

    @Enumerated(EnumType.STRING)
    private StatutCaisse statut; // OUVERTE, FERMEE

    private LocalDateTime dateOuverture;
    private LocalDateTime dateFermeture;

    @ManyToOne
    @JoinColumn(name = "vendeur_id")
    private User vendeur;

    @ManyToOne
    @JoinColumn(name = "boutique_id")
    private Boutique boutique;
}