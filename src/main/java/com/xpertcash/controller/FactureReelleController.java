package com.xpertcash.controller;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.xpertcash.DTOs.FactureReelleDTO;
import com.xpertcash.DTOs.PaiementDTO;
import com.xpertcash.entity.Enum.StatutPaiementFacture;
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
    public ResponseEntity<?> filtrerFacturesParMoisEtAnnee(
            @RequestParam(required = false) Integer mois,
            @RequestParam(required = false) Integer annee,
            HttpServletRequest request) {
        return factureReelleService.filtrerFacturesParMoisEtAnnee(mois, annee, request);
    }

    // ENdpoind Get facture rell by id
    @GetMapping("/factures-reelles/{id}")
    public ResponseEntity<FactureReelleDTO> getFactureReelleById(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        FactureReelleDTO factureDTO = factureReelleService.getFactureReelleById(id, request);
        return ResponseEntity.ok(factureDTO);
    }


    @PostMapping("/factures/{id}/paiement")
    public ResponseEntity<String> enregistrerPaiement(@PathVariable Long id,
                                                    @RequestBody PaiementDTO paiementDTO,
                                                    HttpServletRequest request) {
        try {
            factureReelleService.enregistrerPaiement(
                id,
                paiementDTO.getMontant(),
                paiementDTO.getModePaiement(),
                request
            );
            return ResponseEntity.ok("Paiement enregistré avec succès.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'enregistrement du paiement : " + e.getMessage());
        }
    }


    @GetMapping("/factures/{id}/montant-restant")
    public ResponseEntity<BigDecimal> getMontantRestant(@PathVariable Long id) {
        try {
            BigDecimal montantRestant = factureReelleService.getMontantRestant(id);
            return ResponseEntity.ok(montantRestant);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/factures/{id}/paiements")
    public ResponseEntity<List<PaiementDTO>> getPaiements(@PathVariable Long id) {
        try {
            List<PaiementDTO> paiements = factureReelleService.getPaiementsParFacture(id);
            return ResponseEntity.ok(paiements);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }



}