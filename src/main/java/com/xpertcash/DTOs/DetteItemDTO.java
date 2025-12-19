package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetteItemDTO {

    private Long id;
    private String type;
    // private String source;
    private Double montantInitial; // Montant de départ de la dette
    private Double montantRestant;
    private LocalDateTime date;
    private String description;
    private String numero;
    // Pour FACTURE_IMPAYEE et VENTE_CREDIT : client / contact correspondent au client
    // Pour DEPENSE_DETTE : client / contact correspondent au fournisseur
    private String client;
    private String contact;
    // Pour ENTREE_DETTE : responsable et son contact
    private String responsable;
    private String responsableContact;
}


