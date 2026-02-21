package com.xpertcash.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.xpertcash.entity.VENTE.Vente;

@Data
@Entity
@Table(indexes = {
    @Index(name = "idx_facture_vente_numero", columnList = "numero_facture"),
    @Index(name = "idx_facture_vente_date_emission", columnList = "date_emission"),
    @Index(name = "idx_facture_vente_vente_id", columnList = "vente_id")
})
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