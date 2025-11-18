package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TresorerieDTO {
    
    /**
     * Montant total disponible en caisse (somme des montants courants des caisses ouvertes)
     */
    private Double montantCaisse;
    
    /**
     * Montant total disponible en banque
     * Calculé comme : Entrées (ventes + paiements factures) - Sorties (dépenses + mouvements caisse)
     */
    private Double montantBanque;
    
    /**
     * Montant total disponible en Mobile Money
     * Calculé comme : Entrées (ventes + paiements factures) - Sorties (dépenses + mouvements caisse)
     */
    private Double montantMobileMoney;
    
    /**
     * Montant total des dettes
     * Inclut : Factures impayées + Dépenses avec source DETTE
     */
    private Double montantDette;
    
    /**
     * Total de la trésorerie disponible (Caisse + Banque + Mobile Money)
     */
    private Double totalTresorerie;
    
    /**
     * Détails pour la caisse
     */
    private CaisseDetail caisseDetail;
    
    /**
     * Détails pour la banque
     */
    private BanqueDetail banqueDetail;
    
    /**
     * Détails pour Mobile Money
     */
    private MobileMoneyDetail mobileMoneyDetail;
    
    /**
     * Détails pour les dettes
     */
    private DetteDetail detteDetail;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CaisseDetail {
        /**
         * Nombre de caisses fermées (seules les caisses fermées sont comptabilisées dans la trésorerie)
         */
        private Integer nombreCaissesOuvertes;
        
        /**
         * Montant total en caisse
         */
        private Double montantTotal;
        
        /**
         * Entrées totales (ventes en espèces)
         */
        private Double entrees;
        
        /**
         * Sorties totales (dépenses + retraits)
         */
        private Double sorties;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BanqueDetail {
        /**
         * Entrées totales (ventes + paiements factures avec VIREMENT/CHEQUE)
         */
        private Double entrees;
        
        /**
         * Sorties totales (dépenses générales BANQUE + mouvements caisse DEPENSE avec VIREMENT/CHEQUE)
         */
        private Double sorties;
        
        /**
         * Solde net (entrées - sorties)
         */
        private Double solde;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MobileMoneyDetail {
        /**
         * Entrées totales (ventes + paiements factures avec MOBILE_MONEY)
         */
        private Double entrees;
        
        /**
         * Sorties totales (dépenses générales MOBILE_MONEY + mouvements caisse DEPENSE avec MOBILE_MONEY)
         */
        private Double sorties;
        
        /**
         * Solde net (entrées - sorties)
         */
        private Double solde;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DetteDetail {
        /**
         * Montant total des factures impayées
         */
        private Double facturesImpayees;
        
        /**
         * Nombre de factures impayées
         */
        private Integer nombreFacturesImpayees;
        
        /**
         * Montant total des dépenses avec source DETTE
         */
        private Double depensesDette;
        
        /**
         * Nombre de dépenses avec source DETTE
         */
        private Integer nombreDepensesDette;
        
        /**
         * Total des dettes
         */
        private Double total;
    }
}

