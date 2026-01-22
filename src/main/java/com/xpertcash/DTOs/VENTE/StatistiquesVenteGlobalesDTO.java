package com.xpertcash.DTOs.VENTE;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesVenteGlobalesDTO {
    private List<TopProduitVenduDTO> top3ProduitsVendus;
}
