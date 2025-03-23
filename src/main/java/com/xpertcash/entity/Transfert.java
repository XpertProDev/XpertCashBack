package com.xpertcash.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Transfert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @ManyToOne
    @JoinColumn(name = "boutique_source_id", nullable = false)
    private Boutique boutiqueSource;

    @ManyToOne
    @JoinColumn(name = "boutique_destination_id", nullable = false)
    private Boutique boutiqueDestination;

    private int quantite;

    private LocalDateTime dateTransfert;

    @PrePersist
    public void prePersist() {
        this.dateTransfert = LocalDateTime.now();
    }
}
