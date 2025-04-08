package com.xpertcash.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.StatutFactureProForma;
import com.xpertcash.service.FactureProformaService;

@RestController
@RequestMapping("/api/auth")
public class FactureProformaController {

      @Autowired
    private FactureProformaService factureProformaService;

      @PostMapping("/ajouter")
    public ResponseEntity<?> ajouterFacture(@RequestBody FactureProForma facture) {
        try {
            FactureProForma nouvelleFacture = factureProformaService.ajouterFacture(facture);
            return ResponseEntity.status(HttpStatus.CREATED).body(nouvelleFacture);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    

    @PutMapping("/{id}/statut")
    public ResponseEntity<?> changerStatut(@PathVariable Long id, @RequestParam StatutFactureProForma statut) {
        try {
            FactureProForma facture = factureProformaService.changerStatut(id, statut);
            return ResponseEntity.ok(facture);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

}
