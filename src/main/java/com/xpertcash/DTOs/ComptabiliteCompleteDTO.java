package com.xpertcash.DTOs;

import com.xpertcash.DTOs.VENTE.TransactionSummaryDTO;
import com.xpertcash.DTOs.VENTE.VenteResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComptabiliteCompleteDTO {
    private List<DepenseGeneraleResponseDTO> depensesGenerales;
    private List<CategorieDepenseDTO> categoriesDepense;
    private List<CategorieEntreeDTO> categoriesEntree;
    private List<EntreeGeneraleResponseDTO> entreesGenerales;
    private TransactionSummaryDTO transactionSummary;
    private List<VenteResponse> ventesCaissesFermees;
    private List<PaiementDTO> paiementsFactures;
    private List<TransfertFondsResponseDTO> transfertsFonds;
}

