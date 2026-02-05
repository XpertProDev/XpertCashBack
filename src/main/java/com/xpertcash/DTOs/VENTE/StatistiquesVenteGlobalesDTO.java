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
    
    // Top 3
    private List<TopProduitVenduDTO> top3ProduitsVendus;
    private List<TopVendeurDTO> top3Vendeurs;
    
    // Période appliquée
    private String periode;
}
