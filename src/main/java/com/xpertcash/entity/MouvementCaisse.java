package com.xpertcash.entity;

import lombok.Data;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
public class MouvementCaisse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "caisse_id")
    private Caisse caisse;

    @Enumerated(EnumType.STRING)
    private TypeMouvementCaisse typeMouvement;

    private Double montant;
    private LocalDateTime dateMouvement;
    private String description;

    @Enumerated(EnumType.STRING)
    private ModePaiement modePaiement;
    private Double montantPaye;

    @ManyToOne
    @JoinColumn(name = "vente_id")
    private Vente vente;
}