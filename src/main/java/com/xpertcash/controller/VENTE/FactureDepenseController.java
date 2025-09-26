package com.xpertcash.controller.VENTE;

import com.xpertcash.DTOs.VENTE.FactureDepensePaginatedDTO;
import com.xpertcash.service.VENTE.FactureDepenseService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class FactureDepenseController {

    @Autowired
    private FactureDepenseService factureDepenseService;

    /**
     * Récupère toutes les factures de vente et dépenses avec pagination
     * 
     * @param page Numéro de page (commence à 0)
     * @param size Taille de la page (défaut: 20)
     */
    @GetMapping("/factures-depenses")
    public ResponseEntity<?> getAllFacturesEtDepenses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        try {
            // Validation des paramètres
            if (page < 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Le numéro de page doit être >= 0");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (size <= 0 || size > 100) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "La taille de page doit être entre 1 et 100");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Utiliser des valeurs par défaut pour les autres paramètres
            FactureDepensePaginatedDTO result = factureDepenseService.getAllFacturesEtDepenses(
                page, size, "date", "desc", null, null, "ALL", request);
            
            return ResponseEntity.ok(result);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur interne du serveur : " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Récupère les factures et dépenses du jour avec pagination
     */
    @GetMapping("/factures-depenses/jour")
    public ResponseEntity<?> getFacturesEtDepensesDuJour(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        try {
            FactureDepensePaginatedDTO result = factureDepenseService.getFacturesEtDepensesDuJour(
                page, size, "date", "desc", "ALL", request);
            
            return ResponseEntity.ok(result);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur interne du serveur : " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Récupère les factures et dépenses du mois avec pagination
     */
    @GetMapping("/factures-depenses/mois")
    public ResponseEntity<?> getFacturesEtDepensesDuMois(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        try {
            FactureDepensePaginatedDTO result = factureDepenseService.getFacturesEtDepensesDuMois(
                page, size, "date", "desc", "ALL", request);
            
            return ResponseEntity.ok(result);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur interne du serveur : " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Récupère les factures et dépenses pour une boutique spécifique
     */
    @GetMapping("/factures-depenses/boutique/{boutiqueId}")
    public ResponseEntity<?> getFacturesEtDepensesByBoutique(
            @PathVariable Long boutiqueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        try {
            // Validation des paramètres
            if (page < 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Le numéro de page doit être >= 0");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (size <= 0 || size > 100) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "La taille de page doit être entre 1 et 100");
                return ResponseEntity.badRequest().body(error);
            }
            
            FactureDepensePaginatedDTO result = factureDepenseService.getFacturesEtDepensesByBoutique(
                boutiqueId, page, size, "date", "desc", null, null, "ALL", request);
            
            return ResponseEntity.ok(result);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur interne du serveur : " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Récupère les factures et dépenses pour une caisse spécifique
     */
    @GetMapping("/factures-depenses/caisse/{caisseId}")
    public ResponseEntity<?> getFacturesEtDepensesByCaisse(
            @PathVariable Long caisseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        try {
            // Validation des paramètres
            if (page < 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Le numéro de page doit être >= 0");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (size <= 0 || size > 100) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "La taille de page doit être entre 1 et 100");
                return ResponseEntity.badRequest().body(error);
            }
            
            FactureDepensePaginatedDTO result = factureDepenseService.getFacturesEtDepensesByCaisse(
                caisseId, page, size, "date", "desc", null, null, "ALL", request);
            
            return ResponseEntity.ok(result);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur interne du serveur : " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Récupère toutes les factures de vente d'une entreprise
     */
    @GetMapping("/factures-vente")
    public ResponseEntity<?> getAllFacturesVente(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        try {
            // Validation des paramètres
            if (page < 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Le numéro de page doit être >= 0");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (size <= 0 || size > 100) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "La taille de page doit être entre 1 et 100");
                return ResponseEntity.badRequest().body(error);
            }
            
            FactureDepensePaginatedDTO result = factureDepenseService.getAllFacturesVente(
                page, size, "date", "desc", null, null, request);
            
            return ResponseEntity.ok(result);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur interne du serveur : " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Récupère toutes les factures de vente d'une boutique spécifique
     */
    @GetMapping("/factures-vente/boutique/{boutiqueId}")
    public ResponseEntity<?> getFacturesVenteByBoutique(
            @PathVariable Long boutiqueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        try {
            // Validation des paramètres
            if (page < 0) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Le numéro de page doit être >= 0");
                return ResponseEntity.badRequest().body(error);
            }
            
            if (size <= 0 || size > 100) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "La taille de page doit être entre 1 et 100");
                return ResponseEntity.badRequest().body(error);
            }
            
            FactureDepensePaginatedDTO result = factureDepenseService.getFacturesVenteByBoutique(
                boutiqueId, page, size, "date", "desc", null, null, request);
            
            return ResponseEntity.ok(result);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur interne du serveur : " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Récupère uniquement les totaux (sans pagination) pour un aperçu rapide
     */
    @GetMapping("/factures-depenses/totaux")
    public ResponseEntity<?> getTotauxFacturesEtDepenses(HttpServletRequest request) {
        
        try {
            // Récupérer seulement la première page pour obtenir les totaux
            FactureDepensePaginatedDTO result = factureDepenseService.getAllFacturesEtDepenses(
                0, 1, "date", "desc", null, null, "ALL", request);
            
            // Créer une réponse simplifiée avec seulement les totaux
            Map<String, Object> totaux = new HashMap<>();
            totaux.put("totalFacturesVente", result.getTotalFacturesVente());
            totaux.put("totalDepenses", result.getTotalDepenses());
            totaux.put("soldeNet", result.getSoldeNet());
            totaux.put("nombreFacturesVente", result.getNombreFacturesVente());
            totaux.put("nombreDepenses", result.getNombreDepenses());
            totaux.put("totalElements", result.getTotalElements());
            
            return ResponseEntity.ok(totaux);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur interne du serveur : " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
