package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TresorerieBanquePeriodeDTO {

    private Double soldeActuel;

    private Double caPeriode;

    private Integer nombreTransactions;

    // Métadonnées de pagination
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
        private String type;
        private Double montant;
        private String source;

        private String de;
        private String vers;
    }
}


