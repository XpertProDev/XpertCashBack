package com.xpertcash.controller.VENTE;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.VENTE.VersementComptableDTO;
import com.xpertcash.entity.VENTE.StatutVersement;
import com.xpertcash.service.VENTE.VersementComptableService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;

@RestController
@RequestMapping("/api/auth")
public class VersementComptableController {

    @Autowired
    private  VersementComptableService versementComptableService;


       @GetMapping("/v/boutique/{boutiqueId}")
    public ResponseEntity<List<VersementComptableDTO>> getVersementsParBoutique(
            @PathVariable Long boutiqueId,
            HttpServletRequest request) {
        
       List<VersementComptableDTO> versements =
                versementComptableService.getVersementsDeBoutique(boutiqueId, request);
        
        return ResponseEntity.ok(versements);
    }

   
    @PutMapping("/v/{versementId}/validation")
        public ResponseEntity<VersementComptableDTO> validerVersement(
                @PathVariable Long versementId,
                @RequestBody ValidationVersementRequest requestBody,
                HttpServletRequest request
        ) {
            VersementComptableDTO versement = versementComptableService
                    .validerVersement(versementId, requestBody.isValide(), request);
            return ResponseEntity.ok(versement);
        }

    // 📌 DTO interne juste pour cette requête
    public static class ValidationVersementRequest {
        private boolean valide;

        public boolean isValide() {
            return valide;
        }

        public void setValide(boolean valide) {
            this.valide = valide;
        }
    }



  @PostMapping("/v/boutique/{boutiqueId}/filtre")
public ResponseEntity<?> getVersementsParStatut(
        @PathVariable Long boutiqueId,
        @RequestBody FiltreVersementRequest filtre,
        HttpServletRequest request) {

    StatutVersement statutEnum;
    try {
        statutEnum = StatutVersement.valueOf(filtre.getStatut().toUpperCase());
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(
                Map.of("message", "❌ Statut invalide. Valeurs possibles : EN_ATTENTE, VALIDE, REFUSE")
        );
    }

    List<VersementComptableDTO> versements = versementComptableService
            .getVersementsParStatut(boutiqueId, statutEnum, request);

    if (versements.isEmpty()) {
        String messageLisible;
        switch (statutEnum) {
            case EN_ATTENTE -> messageLisible = "Aucun versement en attente";
            case VALIDE -> messageLisible = "Aucun versement validé";
            case REFUSE -> messageLisible = "Aucun versement refusé";
            default -> messageLisible = "Aucun versement trouvé";
        }
        return ResponseEntity.ok(Map.of("message", messageLisible));
    }

    return ResponseEntity.ok(versements);
}


    @Data
    static class FiltreVersementRequest {
        private String statut; // "EN_ATTENTE", "VALIDE", "REFUSE"
    }


}
