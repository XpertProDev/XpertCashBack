package com.xpertcash.DTOs.VENTE;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CaisseResponseDTO {
    private Long id;
    private Double montantInitial;
    private Double montantCourant;
    private Double montantDette;
    private Double montantEnMain; // Montant réel saisi lors de la fermeture
    private Double ecart; // Différence entre montantCourant et montantEnMain
    private String statut;
    private LocalDateTime dateOuverture;
    private LocalDateTime dateFermeture;
    private Long vendeurId;
    private String nomVendeur;
    private Long boutiqueId;
    private String nomBoutique;
    private List<DepenseResponseDTO> depenses; // Liste des dépenses de la caisse

}