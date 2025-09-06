package com.xpertcash.controller;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import com.xpertcash.DTOs.PaginatedResponseDTO;
import com.xpertcash.DTOs.PaiementDTO;
import com.xpertcash.service.AuthenticationHelper;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.User;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.FactureReelleService;


import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class FactureReelleController {

    @Autowired
    private FactureReelleService factureReelleService;
    @Autowired
    private AuthenticationHelper authHelper;


 
    // Endpoint pour lister toutes les factures réelles (ancienne version pour compatibilité)
    @GetMapping("/mes-factures-reelles")
    public ResponseEntity<List<FactureReelleDTO>> getMesFacturesReelles(HttpServletRequest request) {
        List<FactureReelleDTO> factures = factureReelleService.listerMesFacturesReelles(request);
        return ResponseEntity.ok(factures);
    }

    // Endpoint scalable avec pagination pour lister les factures réelles
    @GetMapping("/mes-factures-reelles/paginated")
    public ResponseEntity<PaginatedResponseDTO<FactureReelleDTO>> getMesFacturesReellesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            PaginatedResponseDTO<FactureReelleDTO> factures = factureReelleService.listerMesFacturesReellesPaginated(page, size, request);
            return ResponseEntity.ok(factures);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new PaginatedResponseDTO<>());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new PaginatedResponseDTO<>());
        }
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
    public ResponseEntity<?> getPaiements(@PathVariable Long id, HttpServletRequest request) {
        try {
            List<PaiementDTO> paiements = factureReelleService.getPaiementsParFacture(id, request);
            return ResponseEntity.ok(paiements);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }



    //Get All impaye facture
   @GetMapping("/factures/impayees")
    public ResponseEntity<List<FactureReelleDTO>> getFacturesImpayees(
        HttpServletRequest request) {
        List<FactureReelleDTO> factures = factureReelleService.listerFacturesImpayees(request);
        return ResponseEntity.ok(factures);
    }

    // Endpoint pour annuler une facture réelle
    @PutMapping("/cancelFacture/{factureId}")
    public ResponseEntity<FactureProForma> annulerFactureReelle(
            @PathVariable Long factureId,
            @RequestBody FactureReelle modifications,
            HttpServletRequest request) {

        // Assigner l'ID reçu dans l'URL à l'objet modifications (pour éviter qu'il soit null)
        modifications.setId(factureId);

        FactureProForma factureAnnulee = factureReelleService.annulerFactureReelle(modifications, request);
        return ResponseEntity.ok(factureAnnulee);
    }



 // Endpoint pour trier

@GetMapping("/par-periode")
public ResponseEntity<?> getFacturesParPeriode(
        @RequestParam String typePeriode,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        HttpServletRequest request
) {
    try {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        // Appel du service pour obtenir les factures sous forme de DTO
        List<FactureReelleDTO> facturesDTO = factureReelleService.getFacturesParPeriode(
                user.getId(), request, typePeriode, dateDebut, dateFin
        );

        // Renvoi de la liste de FactureReelleDTO en réponse
        return ResponseEntity.ok(facturesDTO);
    } catch (Exception e) {
        e.printStackTrace(); // Log pour le développeur
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "status", 500,
                "message", "Une erreur interne est survenue : " + e.getMessage()
        ));
    }
}

// ==================== ENDPOINTS DE GESTION DU CACHE FACTURES RÉELLES ====================

/**
 * Endpoint pour vider le cache des factures réelles
 * 
 * ⚠️  ATTENTION : Ce cache se vide AUTOMATIQUEMENT lors des modifications de factures.
 * Utiliser uniquement pour :
 * - Debug/Test des performances
 * - Maintenance après modifications externes
 * - Résolution de problèmes de cohérence
 * 
 * Ne PAS appeler en routine !
 */
@PostMapping("/cache/evict/factures-reelles")
public ResponseEntity<Map<String, String>> evictFacturesReellesCache() {
    try {
        factureReelleService.evictFacturesReellesCache();
        Map<String, String> response = new HashMap<>();
        response.put("message", "Cache des factures réelles vidé avec succès");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("info", "⚠️ Ce cache se vide automatiquement lors des modifications de factures. Ne pas appeler en routine !");
        return ResponseEntity.ok(response);
    } catch (Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Erreur lors du vidage du cache: " + e.getMessage());
        return ResponseEntity.status(500).body(error);
    }
}

}