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
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public ResponseEntity<CaisseResponseDTO> fermerCaisse(@RequestBody CaisseResponseDTO caisseResponseDTO, HttpServletRequest request) {
        Long boutiqueId = caisseResponseDTO.getBoutiqueId();  // Récupérer le boutiqueId depuis le DTO
        Caisse caisse = caisseService.fermerCaisse(boutiqueId, request); // Appel du service pour fermer la caisse

        // Créer un DTO pour renvoyer les informations de la caisse fermée
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

        // Retourner la réponse avec le DTO
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/active")
    public ResponseEntity<?> getCaisseActive(
        @RequestBody GetCaisseActiveRequest req,
        HttpServletRequest request
) {
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

    // Get tout les caisse ouvert
    @GetMapping("/actives-caisse")
    public ResponseEntity<?> getCaissesActivesBoutique(@RequestBody GetCaisseActiveRequest req, HttpServletRequest request) {
    List<Caisse> caisses = caisseService.getCaissesActivesBoutique(req.getBoutiqueId(), request);

    if (caisses.isEmpty()) {
        return ResponseEntity.status(404).body(Map.of("error", "Aucune caisse ouverte pour cette boutique."));
    }

    List<CaisseResponseDTO> dtos = caisses.stream().map(caisse -> {
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
        return dto;
    }).toList();

    return ResponseEntity.ok(dtos);
}

    //Get tout les caisse
    @GetMapping("/boutique/{boutiqueId}/caisses")
    public ResponseEntity<?> getToutesLesCaisses(
            @PathVariable Long boutiqueId,
            HttpServletRequest request
    ) {
        List<Caisse> caisses = caisseService.getToutesLesCaisses(boutiqueId, request);

        List<CaisseResponseDTO> dtos = caisses.stream().map(c -> {
            CaisseResponseDTO dto = new CaisseResponseDTO();
            dto.setId(c.getId());
            dto.setMontantInitial(c.getMontantInitial());
            dto.setMontantCourant(c.getMontantCourant());
            dto.setStatut(c.getStatut().name());
            dto.setDateOuverture(c.getDateOuverture());
            dto.setDateFermeture(c.getDateFermeture());
            dto.setVendeurId(c.getVendeur() != null ? c.getVendeur().getId() : null);
            dto.setNomVendeur(c.getVendeur() != null ? c.getVendeur().getNomComplet() : null);
            dto.setBoutiqueId(c.getBoutique() != null ? c.getBoutique().getId() : null);
            dto.setNomBoutique(c.getBoutique() != null ? c.getBoutique().getNomBoutique() : null);
            return dto;
        }).toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/vendeur/{vendeurId}")
    public ResponseEntity<List<CaisseResponseDTO>> getCaissesByVendeur(
            @PathVariable Long vendeurId,
            HttpServletRequest request
    ) {
        List<CaisseResponseDTO> caisses = caisseService.getCaissesByVendeur(vendeurId, request);
        return ResponseEntity.ok(caisses);
    }


    @GetMapping("/caisse/derniere/{boutiqueId}")
    public ResponseEntity<?> getDerniereCaisseVendeur(
            @PathVariable Long boutiqueId,
            HttpServletRequest request) {

        Optional<CaisseResponseDTO> caisseOpt = caisseService.getDerniereCaisseVendeur(boutiqueId, request);

        if (caisseOpt.isEmpty()) {
            // Message clair si aucune caisse n'a été trouvée
            return ResponseEntity.ok("Aucune caisse trouvée pour ce vendeur dans cette boutique.");
        }
        return ResponseEntity.ok(caisseOpt.get());
    }

    // Suivre Fluidité dargent encour dactivite
    @GetMapping("/macaisse/actuelle/{boutiqueId}")
    public ResponseEntity<CaisseResponseDTO> getEtatActuelCaisse(
            @PathVariable Long boutiqueId,
            HttpServletRequest request) {
        CaisseResponseDTO dto = caisseService.getEtatActuelCaisse(boutiqueId, request);
        return ResponseEntity.ok(dto);
    }


}