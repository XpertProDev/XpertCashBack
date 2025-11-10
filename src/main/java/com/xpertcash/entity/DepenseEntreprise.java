package com.xpertcash.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "depense_entreprise")
@Getter
@Setter
public class DepenseEntreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cree_par_id")
    private User creePar;

    private Double montant;

    private String motif;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private ModePaiement modePaiement;

    private LocalDateTime dateDepense;
}

