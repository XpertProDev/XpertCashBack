package com.xpertcash.entity.PROSPECT;

import java.time.LocalDateTime;

import com.xpertcash.entity.Produit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProspectAchat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospect_id")
    private Prospect prospect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id")
    private Produit produit;

    private Integer quantite;
    private Double montantAchat;
    @Column(length = 2000)
    private String notesAchat;
    private LocalDateTime dateAchat = LocalDateTime.now();
    
    // Informations sur le client créé
    private Long clientId;
    private String clientType; 
}
