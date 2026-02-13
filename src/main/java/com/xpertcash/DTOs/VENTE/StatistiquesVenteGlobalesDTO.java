package com.xpertcash.DTOs.VENTE;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesVenteGlobalesDTO {
    // Statistiques globales
    private Long totalVentes;           // Nombre total de ventes
    private Long nombreArticles;        // Nombre total d'articles vendus
    private Double montantTotal;        // Montant total des ventes
    
    // Montants par statut de caisse
    private Double montantCaisseOuverte;   // Montant des ventes dans les caisses ouvertes
    private Double montantCaisseFermee;    // Montant des ventes dans les caisses fermées
    
    // Tous les produits vendus (triés par quantité décroissante - les 3 premiers sont le "top 3")
    private List<TopProduitVenduDTO> produitsVendus;
    
    // Tous les vendeurs (triés par montant décroissant - les 3 premiers sont le "top 3")
    private List<TopVendeurDTO> vendeurs;
    
    // Période appliquée
    private String periode;
}
