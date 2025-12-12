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
public class Paiement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal montant;

    private LocalDateTime datePaiement;

     private String modePaiement;

    @ManyToOne
    @JoinColumn(name = "facture_reelle_id")
    private FactureReelle factureReelle;

    @ManyToOne
    @JoinColumn(name = "encaisse_par_id")
    private User encaissePar;

}
