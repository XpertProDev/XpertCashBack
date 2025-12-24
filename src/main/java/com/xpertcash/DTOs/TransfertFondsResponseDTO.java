package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransfertFondsResponseDTO {
    private Long id;
    private LocalDateTime dateTransfert;
    private String motif;
    private String description; // Description explicite pour SORTIE ou ENTREE
    private String responsable;
    private String de;
    private String vers;
    private Double montant;
    private String personneALivrer;
    private Long entrepriseId;
    private String entrepriseNom;
    private String typeTransaction; // SORTIE ou ENTREE pour les transferts
    private String sensTransfert; // "SORTIE" ou "ENTREE" pour indiquer le sens du transfert
    private String origine; // Source ou destination selon le sens
    private String pieceJointe; // URL ou chemin du fichier (facultatif)
}

