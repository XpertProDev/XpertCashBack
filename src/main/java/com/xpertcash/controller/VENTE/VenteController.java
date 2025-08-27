package com.xpertcash.controller.VENTE;

import com.xpertcash.DTOs.VENTE.RemboursementRequest;
import com.xpertcash.DTOs.VENTE.RemboursementResponse;
import com.xpertcash.DTOs.VENTE.VenteRequest;
import com.xpertcash.DTOs.VENTE.VenteResponse;
import com.xpertcash.service.VENTE.VenteService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class VenteController {

    @Autowired
    private VenteService venteService;

    @PostMapping("/vente/enregistrer")
    public ResponseEntity<VenteResponse> enregistrerVente(@RequestBody VenteRequest request, HttpServletRequest httpRequest) {
        VenteResponse response = venteService.enregistrerVente(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    //Remboursement
    @PostMapping("/vente/rembourser")
    public ResponseEntity<?> rembourserVente(@RequestBody RemboursementRequest request, HttpServletRequest httpRequest) {
        try {
            VenteResponse response = venteService.rembourserVente(request, httpRequest);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Remboursement effectué avec succès",
                "data", response
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Erreur interne du serveur : " + e.getMessage()
            ));
        }
    }


        // Lister les remboursements d'une vente
        @GetMapping("/mes/remboursements")
        public ResponseEntity<List<RemboursementResponse>> getMesRemboursements(
                HttpServletRequest httpRequest) {
            String jwtToken = httpRequest.getHeader("Authorization");
            List<RemboursementResponse> remboursements = venteService.getMesRemboursements(jwtToken);
            return ResponseEntity.ok(remboursements);
        }





    @GetMapping("/vente/{id}")
        public ResponseEntity<VenteResponse> getVenteById(
                @PathVariable Long id,
                HttpServletRequest httpRequest
        ) {
            VenteResponse response = venteService.getVenteById(id, httpRequest);
            return ResponseEntity.ok(response);
        }


    @GetMapping("/vente")
    public ResponseEntity<java.util.List<VenteResponse>> getAllVentes() {
        java.util.List<VenteResponse> ventes = venteService.getAllVentes();
        return ResponseEntity.ok(ventes);
    }

    @GetMapping("/vente/boutique/{boutiqueId}")
        public ResponseEntity<List<VenteResponse>> getVentesByBoutique(
            @PathVariable Long boutiqueId,
            HttpServletRequest request
    ) {
        List<VenteResponse> ventes = venteService.getVentesByBoutique(boutiqueId, request);
        return ResponseEntity.ok(ventes);
    }


    @GetMapping("/vente/vendeur/{vendeurId}")
   public ResponseEntity<List<VenteResponse>> getVentesByVendeur(
        @PathVariable Long vendeurId,
        HttpServletRequest request
        ) {
            List<VenteResponse> ventes = venteService.getVentesByVendeur(vendeurId, request);
            return ResponseEntity.ok(ventes);
        }

          // Montant total des ventes du jour (avec remboursements)
    @GetMapping("/vente/montant-total-jour")
    public ResponseEntity<Double> getMontantTotalVentesDuJour(HttpServletRequest request) {
        double montantTotal = venteService.getMontantTotalVentesDuJourConnecte(request);
        return ResponseEntity.ok(montantTotal);
    }

    // Montant total des ventes du mois (avec remboursements)
    @GetMapping("/vente/montant-total-mois")
    public ResponseEntity<Double> getMontantTotalVentesDuMois(HttpServletRequest request) {
        double montantTotal = venteService.getMontantTotalVentesDuMoisConnecte(request);
        return ResponseEntity.ok(montantTotal);
    }


    // Global benefiche
    @GetMapping("/vente/benefice-net")
    public ResponseEntity<Double> getBeneficeNet(HttpServletRequest request) {
        double beneficeNet = venteService.calculerBeneficeNetEntrepriseConnecte(request);
        return ResponseEntity.ok(beneficeNet);
    }

    // Bénéfice net du jour
    @GetMapping("/vente/benefice/jour")
    public ResponseEntity<Double> getBeneficeNetDuJour(HttpServletRequest request) {
        double benefice = venteService.calculerBeneficeNetDuJourConnecte(request);
        return ResponseEntity.ok(benefice);
    }

    // Bénéfice net du mois
    @GetMapping("/vente/benefice/mois")
    public ResponseEntity<Double> getBeneficeNetDuMois(HttpServletRequest request) {
        double benefice = venteService.calculerBeneficeNetDuMoisConnecte(request);
        return ResponseEntity.ok(benefice);
    }

    // Benefice annuel
    @GetMapping("/vente/benefice/annee")
    public ResponseEntity<Double> getBeneficeNetAnnuel(HttpServletRequest request) {
        double benefice = venteService.calculerBeneficeNetAnnuelConnecte(request);
        return ResponseEntity.ok(benefice);
    }

    

}