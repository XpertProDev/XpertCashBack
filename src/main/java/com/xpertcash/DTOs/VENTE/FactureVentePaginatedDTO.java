package com.xpertcash.DTOs.VENTE;

import lombok.Data;
import java.util.List;

@Data
public class FactureVentePaginatedDTO {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean hasNext;
    private boolean hasPrevious;
    private List<FactureVenteResponseDTO> items;
    private double totalMontantFactures;
    private int nombreFactures;
    private int nombreFacturesRemboursees;
    private int nombreFacturesPartiellementRemboursees;
    private int nombreFacturesNormales;
    private List<VendeurFactureDTO> vendeurs;
}
