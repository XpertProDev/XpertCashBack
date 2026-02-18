package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriVenteRequest {
    /** true = afficher en premier au POS pour cette boutique. */
    private Boolean favori;
    /** Ordre parmi les favoris (plus petit = en premier). Optionnel. */
    private Integer ordre;
}
