package com.xpertcash.controller.VENTE;

import com.xpertcash.entity.Caisse;
import com.xpertcash.service.VENTE.CaisseService;
import com.xpertcash.DTOs.VENTE.CaisseResponseDTO;
import com.xpertcash.DTOs.VENTE.OuvrirCaisseRequest;
import com.xpertcash.DTOs.VENTE.FermerCaisseRequest;
import com.xpertcash.DTOs.VENTE.GetCaisseActiveRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class CaisseController {
    @Autowired
    private CaisseService caisseService;

    @PostMapping("/ouvrir")
    public ResponseEntity<CaisseResponseDTO> ouvrirCaisse(@RequestBody OuvrirCaisseRequest req, HttpServletRequest request) {
        Caisse caisse = caisseService.ouvrirCaisse(req.getBoutiqueId(), req.getMontantInitial(), request);
        CaisseResponseDTO dto = new CaisseResponseDTO();
        dto.setId(caisse.getId());
        dto.setMontantInitial(caisse.getMontantInitial());
        dto.setMontantCourant(caisse.getMontantCourant());
        dto.setStatut(caisse.getStatut().name());
        dto.setDateOuverture(caisse.getDateOuverture());
        dto.setDateFermeture(caisse.getDateFermeture());
        dto.setVendeurId(caisse.getVendeur() != null ? caisse.getVendeur().getId() : null);
        dto.setNomVendeur(caisse.getVendeur() != null ? caisse.getVendeur().getNomComplet() : null);
        dto.setBoutiqueId(caisse.getBoutique() != null ? caisse.getBoutique().getId() : null);
        dto.setNomBoutique(caisse.getBoutique() != null ? caisse.getBoutique().getNomBoutique() : null);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/fermer")
    public ResponseEntity<CaisseResponseDTO> fermerCaisse(@RequestBody FermerCaisseRequest req, HttpServletRequest request) {
        Caisse caisse = caisseService.fermerCaisse(req.getCaisseId(), request);
        CaisseResponseDTO dto = new CaisseResponseDTO();
        dto.setId(caisse.getId());
        dto.setMontantInitial(caisse.getMontantInitial());
        dto.setMontantCourant(caisse.getMontantCourant());
        dto.setStatut(caisse.getStatut().name());
        dto.setDateOuverture(caisse.getDateOuverture());
        dto.setDateFermeture(caisse.getDateFermeture());
        dto.setVendeurId(caisse.getVendeur() != null ? caisse.getVendeur().getId() : null);
        dto.setNomVendeur(caisse.getVendeur() != null ? caisse.getVendeur().getNomComplet() : null);
        dto.setBoutiqueId(caisse.getBoutique() != null ? caisse.getBoutique().getId() : null);
        dto.setNomBoutique(caisse.getBoutique() != null ? caisse.getBoutique().getNomBoutique() : null);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/active")
    public ResponseEntity<?> getCaisseActive(@RequestBody GetCaisseActiveRequest req, HttpServletRequest request) {
        var opt = caisseService.getCaisseActive(req.getBoutiqueId(), request);
        if (opt.isPresent()) {
            Caisse caisse = opt.get();
            CaisseResponseDTO dto = new CaisseResponseDTO();
            dto.setId(caisse.getId());
            dto.setMontantInitial(caisse.getMontantInitial());
            dto.setMontantCourant(caisse.getMontantCourant());
            dto.setStatut(caisse.getStatut().name());
            dto.setDateOuverture(caisse.getDateOuverture());
            dto.setDateFermeture(caisse.getDateFermeture());
            dto.setVendeurId(caisse.getVendeur() != null ? caisse.getVendeur().getId() : null);
            dto.setNomVendeur(caisse.getVendeur() != null ? caisse.getVendeur().getNomComplet() : null);
            dto.setBoutiqueId(caisse.getBoutique() != null ? caisse.getBoutique().getId() : null);
            dto.setNomBoutique(caisse.getBoutique() != null ? caisse.getBoutique().getNomBoutique() : null);
            return ResponseEntity.ok(dto);
        } else {
            HashMap<String, String> error = new HashMap<>();
            error.put("error", "Aucune caisse ouverte pour ce vendeur dans cette boutique.");
            return ResponseEntity.status(404).body(error);
        }
    }
}