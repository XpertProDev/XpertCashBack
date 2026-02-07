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
    
    // Infos auteur
    private Long auteurId;
    private String auteurNom;
    private String photoAuteur;
    
    // Infos facture
    private Long factureId;
    private String numeroFacture;
    private String statutFacture;
}
