package com.xpertcash.entity;

import lombok.Data;
import jakarta.persistence.*;       
import java.time.LocalDateTime;

@Data
@Entity
public class VenteHistorique {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "vente_id")
    private Vente vente;

    private LocalDateTime dateAction;
    private String action;
    private String details;
}