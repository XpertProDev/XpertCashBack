package com.xpertcash.DTOs;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistoriqueNoteProformaDTO {
    private Long id;
    private String numeroIdentifiant;
    private String contenu;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDerniereModification;
    private boolean modifiee;
    
    // Infos auteur (qui a créé la note)
    private Long auteurId;
    private String auteurNom;
    private String photoAuteur;
    
    // Infos destinataire (à qui la note est assignée)
    private Long destinataireId;
    private String destinataireNom;
    
    // Infos facture
    private Long factureId;
    private String numeroFacture;
    private String statutFacture;
}
