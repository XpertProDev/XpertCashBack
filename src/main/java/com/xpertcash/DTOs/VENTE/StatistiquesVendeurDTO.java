package com.xpertcash.DTOs.VENTE;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesVendeurDTO {
    // Informations du vendeur
    private Long vendeurId;
    private String nomVendeur;
    private String emailVendeur;
    private String telephoneVendeur;
    
    // Statistiques de vente
    private Long totalVentes;
    private Long nombreArticles;
    private Double montantTotal;
    
    // Montants par statut de caisse
    private Double montantCaisseOuverte;
    private Double montantCaisseFermee;
    
    // Tous les produits vendus par ce vendeur (triés par quantité décroissante)
    private List<TopProduitVenduDTO> produitsVendus;
    
    // Période appliquée
    private String periode;
}
