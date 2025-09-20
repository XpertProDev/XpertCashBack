package com.xpertcash.DTOs.VENTE;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FermerCaisseResponseDTO {
    private Long id;
    private Double montantInitial;
    private Double montantCourant; // Montant théorique avant fermeture
    private Double montantEnMain; // Montant réel saisi par le vendeur
    private Double ecart; // Différence entre montantCourant et montantEnMain
    private String statut;
    private LocalDateTime dateOuverture;
    private LocalDateTime dateFermeture;
    private Long vendeurId;
    private String nomVendeur;
    private Long boutiqueId;
    private String nomBoutique;
    
    // Statistiques des dépenses
    private Double totalDepenses;
    private Integer nombreDepenses;
}
