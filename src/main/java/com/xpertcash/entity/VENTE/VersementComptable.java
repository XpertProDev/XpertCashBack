package com.xpertcash.entity.VENTE;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

import java.time.LocalDateTime;

import com.xpertcash.entity.Caisse;
import com.xpertcash.entity.User;

import jakarta.persistence.*;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Data
@Entity
public class VersementComptable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Caisse concernée
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caisse_id", nullable = false)
    private Caisse caisse;

    // 💰 Montant à verser
    @Column(nullable = false)
    private Double montant;

    // 📅 Date du versement
    @Column(nullable = false)
    private LocalDateTime dateVersement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutVersement statut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_id", nullable = false)
    private User creePar;

    // ✅ Nouveau : utilisateur qui valide le versement
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private User validePar;

    // ✅ Nouveau : date de validation
    private LocalDateTime dateValidation;
}

