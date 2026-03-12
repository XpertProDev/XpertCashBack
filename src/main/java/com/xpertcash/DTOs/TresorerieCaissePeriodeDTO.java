package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TresorerieCaissePeriodeDTO {

    /**
     * Solde actuel de la caisse (montantCaisse global de la trésorerie).
     */
    private Double soldeActuel;

    /**
     * CA (entrées) sur la période sélectionnée, côté caisse.
     */
    private Double caPeriode;

    /**
     * Nombre total de transactions de caisse sur la période (entrées + sorties).
     */
    private Integer nombreTransactions;

    // Métadonnées de pagination (comme en comptabilité)
    private Integer pageNumber;
    private Integer pageSize;
    private Integer totalElements;
    private Integer totalPages;
    private Boolean hasNext;
    private Boolean hasPrevious;
    private Boolean first;
    private Boolean last;

    private List<LigneTransaction> transactions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LigneTransaction {
        private LocalDateTime date;
        private String designation;
        private String type;   // ex: ENTREE, ENTREE_CAISSE, PAIEMENT_FACTURE, DEPENSE_CAISSE, RETRAIT_CAISSE, TRANSFERT
        private Double montant;
        private String source; // ex: CAISSE, FERMETURE_CAISSE, TRANSFERT_VERS, TRANSFERT_DEPUIS, ESPECES, MOBILE_MONEY

        // Pour les transferts : de où vers où (ex: CAISSE -> BANQUE)
        private String de;
        private String vers;
    }
}

