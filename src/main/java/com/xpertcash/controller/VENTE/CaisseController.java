package com.xpertcash.controller.VENTE;

import com.xpertcash.entity.Caisse;
import com.xpertcash.service.VENTE.CaisseService;
import com.xpertcash.DTOs.VENTE.CaisseResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class CaisseController {
    @Autowired
    private CaisseService caisseService;

    @PostMapping("/ouvrir")
    public ResponseEntity<CaisseResponseDTO> ouvrirCaisse(@RequestParam Long boutiqueId, @RequestParam(required = false) Double montantInitial, HttpServletRequest request) {
        Caisse caisse = caisseService.ouvrirCaisse(boutiqueId, montantInitial, request);
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
    public ResponseEntity<CaisseResponseDTO> fermerCaisse(@RequestParam Long caisseId, HttpServletRequest request) {
        Caisse caisse = caisseService.fermerCaisse(caisseId, request);
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
    public ResponseEntity<CaisseResponseDTO> getCaisseActive(@RequestParam Long boutiqueId, HttpServletRequest request) {
        return caisseService.getCaisseActive(boutiqueId, request)
                .map(caisse -> {
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
                })
                .orElse(ResponseEntity.notFound().build());
    }
}