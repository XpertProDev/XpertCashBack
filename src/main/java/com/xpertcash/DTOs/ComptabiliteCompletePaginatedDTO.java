package com.xpertcash.DTOs;

import com.xpertcash.DTOs.VENTE.TransactionSummaryDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComptabiliteCompletePaginatedDTO {
    private List<Object> transactions; // Liste combinée de toutes les transactions paginées
    private List<CategorieDepenseDTO> categoriesDepense;
    private List<CategorieEntreeDTO> categoriesEntree;
    private TransactionSummaryDTO transactionSummary;
    
    // Informations de pagination
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean isFirst;
    private boolean isLast;
}

