package com.xpertcash.controller.VENTE;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.VENTE.FactureVentePaginatedDTO;
import com.xpertcash.service.VENTE.FactureVenteService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class FactureVenteController {

    @Autowired
    private FactureVenteService factureVenteService;

    

    @GetMapping("/factureVente/entreprise/paginated")
    public ResponseEntity<FactureVentePaginatedDTO> getFacturesForEntrepriseWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dateEmission") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            HttpServletRequest request) {
        
        FactureVentePaginatedDTO factures = factureVenteService.getAllFacturesWithPagination(
            page, size, sortBy, sortDir, request);
        return ResponseEntity.ok(factures);
    }

}
