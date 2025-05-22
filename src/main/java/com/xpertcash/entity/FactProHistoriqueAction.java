package com.xpertcash.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
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

    private String action;

    private LocalDateTime dateAction;
    @Column(precision = 15, scale = 2)
    private BigDecimal montantFacture;


    @ManyToOne
    private User utilisateur;

    @ManyToOne
    private FactureProForma facture;

    private String details; 


}
