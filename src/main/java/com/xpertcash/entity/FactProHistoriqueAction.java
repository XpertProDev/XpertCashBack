package com.xpertcash.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactProHistoriqueAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action; // "Création", "Modification", "Approbation"

    private LocalDateTime dateAction;

    @ManyToOne
    private User utilisateur;

    @ManyToOne
    private FactureProForma facture;

    private String details; 

}
