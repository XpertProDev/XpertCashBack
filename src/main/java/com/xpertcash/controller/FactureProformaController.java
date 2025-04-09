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

    // Endpoint pour ajouter une facture pro forma
    @PostMapping("/ajouter")
    public ResponseEntity<?> ajouterFacture(
            @RequestBody FactureProForma facture,
            @RequestParam(defaultValue = "0") Double remisePourcentage,
            @RequestParam(defaultValue = "false") Boolean appliquerTVA) { 
        try {
            // Appel du service pour ajouter la facture
            FactureProForma nouvelleFacture = factureProformaService.ajouterFacture(facture, remisePourcentage, appliquerTVA);
    
            // Retourner la facture créée en réponse HTTP 201 (CREATED)
            return ResponseEntity.status(HttpStatus.CREATED).body(nouvelleFacture);
        } catch (RuntimeException e) {
            // Retourner l'erreur en réponse HTTP 400 (BAD REQUEST) si une exception est levée
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


    // Endpoint pour modifier une facture pro forma
    @PutMapping("/updatefacture/{factureId}")
    public ResponseEntity<FactureProForma> updateFacture(
            @PathVariable Long factureId,
            @RequestParam(required = false) Double remisePourcentage,
            @RequestParam(required = false) Boolean appliquerTVA,
            @RequestBody FactureProForma modifications) {
    
        FactureProForma factureModifiee = factureProformaService.modifierFacture(factureId, remisePourcentage, appliquerTVA, modifications);
        return ResponseEntity.ok(factureModifiee);
    }
    

}
