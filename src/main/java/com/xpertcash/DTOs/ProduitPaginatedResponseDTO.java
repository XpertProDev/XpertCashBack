package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import com.xpertcash.DTOs.PRODUIT.ProduitDetailsResponseDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProduitPaginatedResponseDTO {
    private List<ProduitDetailsResponseDTO> produits;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean isFirst;
    private boolean isLast;

    public static ProduitPaginatedResponseDTO fromPage(Page<ProduitDetailsResponseDTO> page) {
        return new ProduitPaginatedResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.isFirst(),
                page.isLast()
        );
    }
}
