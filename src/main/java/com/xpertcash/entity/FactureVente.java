package com.xpertcash.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
public class FactureVente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "vente_id")
    private Vente vente;

    private String numeroFacture;
    private LocalDateTime dateEmission;
    private Double montantTotal;
}