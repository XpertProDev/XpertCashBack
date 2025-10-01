package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatistiquesGlobalesDTO {
    private ProduitsStats produits;
    private VentesStats ventes;
    private BeneficesStats benefices;
    private UtilisateursStats utilisateurs;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProduitsStats {
        private long total;
        private long enStock;
        private long horsStock;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VentesStats {
        private double jour;
        private double mois;
        private double annuel;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BeneficesStats {
        private double jour;
        private double mois;
        private double annuel;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UtilisateursStats {
        private long total;
        private long vendeurs;
    }
}

