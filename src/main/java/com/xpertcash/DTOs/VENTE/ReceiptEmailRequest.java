package com.xpertcash.DTOs.VENTE;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReceiptEmailRequest {
    private String email;
    private String venteId;
    private String numeroFacture;
    private LocalDateTime dateVente;
    private BigDecimal montantTotal;
    private String modePaiement;
    private BigDecimal montantPaye;
    private BigDecimal changeDue;
    private String nomVendeur;
    private String nomBoutique;
    private List<VenteLigneResponse> lignes;
    private Double remiseGlobale;
    private Map<Long, Double> remisesProduits;
}