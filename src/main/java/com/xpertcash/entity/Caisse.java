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
    private Double montantEnMain;
    private Double ecart;

    @Enumerated(EnumType.STRING)
    private StatutCaisse statut;

    private LocalDateTime dateOuverture;
    private LocalDateTime dateFermeture;

    @ManyToOne
    @JoinColumn(name = "vendeur_id")
    private User vendeur;

    @ManyToOne
    @JoinColumn(name = "boutique_id")
    private Boutique boutique;
}