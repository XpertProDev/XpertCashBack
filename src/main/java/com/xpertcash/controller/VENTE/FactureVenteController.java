package com.xpertcash.controller.VENTE;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.VENTE.FactureVenteResponseDTO;
import com.xpertcash.service.VENTE.FactureVenteService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class FactureVenteController {

    @Autowired
    private FactureVenteService factureVenteService;

    
    @GetMapping("/factureVente/entreprise")
   public ResponseEntity<List<FactureVenteResponseDTO>> getFacturesForEntreprise(HttpServletRequest request) {
        List<FactureVenteResponseDTO> factures = factureVenteService.getAllFacturesForConnectedUser(request);
        return ResponseEntity.ok(factures);
    }

}
