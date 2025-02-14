package com.xpertcash.entity;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Entity
@Data
public class Depense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Le montant de la dépense est obligatoire")
    private double montant;  // Montant de la dépense

    @NotNull(message = "La description de la dépense est obligatoire")
    private String description;  // Description de la dépense (par exemple, approvisionnement en stock)

    private Date date;  // Date de la dépense

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "entreprise_id", nullable = false)  // Association avec Entreprise
    @JsonBackReference
    private Entreprise entreprise;

}

