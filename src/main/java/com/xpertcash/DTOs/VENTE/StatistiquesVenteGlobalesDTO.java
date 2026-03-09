package com.xpertcash.DTOs.VENTE;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesVenteGlobalesDTO {
    private Long totalVentes;
    private Long totalRembourse;
    private Long nombreArticles;
    private Double montantTotalBrut;
    private Double montantTotal;
    
    private Double montantCaisseOuverte;
    private Double montantCaisseFermee;
    private List<TopProduitVenduDTO> produitsVendus;
    private List<TopVendeurDTO> vendeurs;
    private String periode;
}
