package com.xpertcash.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.FactureReelleDTO;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.StatutPaiementFacture;
import com.xpertcash.service.FactureReelleService;


import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class FactureReelleController {

    @Autowired
    private FactureReelleService factureReelleService;

    @PutMapping("/updatefacture/{id}/modifier-statut")
    public ResponseEntity<FactureReelleDTO> modifierStatut(@PathVariable Long id, 
        @RequestParam StatutPaiementFacture nouveauStatut, HttpServletRequest request) {
        
        FactureReelleDTO factureModifiee = factureReelleService.modifierStatutPaiement(id, nouveauStatut, request);
        
        return ResponseEntity.ok(factureModifiee);
    }

    // Endpoint pour lister tout les factures reelles
      @GetMapping("/mes-factures-reelles")
        public ResponseEntity<List<FactureReelleDTO>> getMesFacturesReelles(HttpServletRequest request) {
            List<FactureReelleDTO> factures = factureReelleService.listerMesFacturesReelles(request);
            return ResponseEntity.ok(factures);
        }

    // Endpoint pour trier les factures par mois/année
    @GetMapping("/filtrer-facturesReelles")
    public ResponseEntity<List<FactureReelleDTO>> filtrerFacturesParMoisEtAnnee(
            @RequestParam(required = false) Integer mois,
            @RequestParam(required = false) Integer annee) {

        List<FactureReelleDTO> factures = factureReelleService.filtrerFacturesParMoisEtAnnee(mois, annee);
        return ResponseEntity.ok(factures);
    }

}
