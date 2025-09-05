package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactureProformaPaginatedResponseDTO {
    private List<Map<String, Object>> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
    private boolean isFirst;
    private boolean isLast;
    private long totalFactures;
    private long totalFacturesBrouillon;
    private long totalFacturesEnAttente;
    private long totalFacturesValidees;
    private long totalFacturesAnnulees;

    public static FactureProformaPaginatedResponseDTO fromPage(Page<Map<String, Object>> page, 
                                                              long totalFactures,
                                                              long totalFacturesBrouillon,
                                                              long totalFacturesEnAttente,
                                                              long totalFacturesValidees,
                                                              long totalFacturesAnnulees) {
        return new FactureProformaPaginatedResponseDTO(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious(),
                page.isFirst(),
                page.isLast(),
                totalFactures,
                totalFacturesBrouillon,
                totalFacturesEnAttente,
                totalFacturesValidees,
                totalFacturesAnnulees
        );
    }
}
