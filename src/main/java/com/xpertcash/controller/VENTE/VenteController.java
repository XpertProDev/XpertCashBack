package com.xpertcash.controller.VENTE;

import com.xpertcash.DTOs.VENTE.RemboursementRequest;
import com.xpertcash.DTOs.VENTE.RemboursementResponse;
import com.xpertcash.DTOs.VENTE.VenteRequest;
import com.xpertcash.DTOs.VENTE.VenteResponse;
import com.xpertcash.service.VENTE.VenteService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

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
    public ResponseEntity<VenteResponse> rembourserVente(@RequestBody RemboursementRequest request, HttpServletRequest httpRequest) {
        VenteResponse response = venteService.rembourserVente(request, httpRequest);
        return ResponseEntity.ok(response);
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
    public ResponseEntity<VenteResponse> getVenteById(@PathVariable Long id) {
        VenteResponse response = venteService.getVenteById(id);
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
}