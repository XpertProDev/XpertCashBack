package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitStockPaginatedResponseDTO {
    private List<ProduitDTO> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean isFirst;
    private boolean isLast;
    private long totalProduitsActifs;
    private long totalProduitsEnStock;
    private long totalProduitsHorsStock;

    public static ProduitStockPaginatedResponseDTO fromPage(Page<ProduitDTO> page, long totalProduitsActifs, long totalProduitsEnStock, long totalProduitsHorsStock) {
        return new ProduitStockPaginatedResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.isFirst(),
                page.isLast(),
                totalProduitsActifs,
                totalProduitsEnStock,
                totalProduitsHorsStock
        );
    }
}
