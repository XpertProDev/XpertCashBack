package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitEntreprisePaginatedResponseDTO {
    private List<ProduitDTO> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean isFirst;
    private boolean isLast;
    private long totalProduitsUniques;
    private long totalBoutiques;

    public static ProduitEntreprisePaginatedResponseDTO fromPage(Page<ProduitDTO> page, long totalProduitsUniques, long totalBoutiques) {
        return new ProduitEntreprisePaginatedResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.isFirst(),
                page.isLast(),
                totalProduitsUniques,
                totalBoutiques
        );
    }
}
