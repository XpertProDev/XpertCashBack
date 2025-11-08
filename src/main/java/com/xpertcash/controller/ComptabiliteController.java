package com.xpertcash.controller;

import com.xpertcash.DTOs.ComptabiliteDTO;
import com.xpertcash.DTOs.DepenseGeneraleRequest;
import com.xpertcash.DTOs.DepenseGeneraleResponse;
import com.xpertcash.service.ComptabiliteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ComptabiliteController {

    @Autowired
    private ComptabiliteService comptabiliteService;

    /**
     * Endpoint qui retourne toutes les données comptables de l'entreprise
     * 
     * Retourne:
     * {
     *   chiffreAffaires: { total, duJour, duMois, deLAnnee, totalVentes, totalFactures, totalPaiementsFactures },
     *   ventes: { nombreTotal, montantTotal, duJour, montantDuJour, duMois, montantDuMois, deLAnnee, montantDeLAnnee },
     *   facturation: { nombreTotalFactures, montantTotalFactures, montantPaye, montantImpaye, duJour, ... },
     *   depenses: { nombreTotal, montantTotal, duJour, montantDuJour, ... },
     *   boutiques: [ { id, nom, chiffreAffaires, nombreVentes, totalDepenses, nombreDepenses } ],
     *   clients: { nombreTotal, actifs, montantTotalAchete, meilleursClients: [ Top 3 ] },
     *   vendeurs: { nombreTotal, actifs, chiffreAffairesTotal, meilleursVendeurs: [ Top 3 ] },
     *   activites: { nombreVentesTotal, nombreFacturesTotal, nombreDepensesTotal, nombreTransactionsJour }
     * }
     */
    @GetMapping("/comptabilite")
    public ResponseEntity<?> getComptabilite(HttpServletRequest request) {
        try {
            ComptabiliteDTO comptabilite = comptabiliteService.getComptabilite(request);
            return ResponseEntity.ok(comptabilite);
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
     * Endpoint pour enregistrer une dépense générale (réservé au rôle COMPTABLE)
     */
    @PostMapping("/comptabilite/depenses-generales")
    public ResponseEntity<?> enregistrerDepenseGenerale(@RequestBody DepenseGeneraleRequest request,
                                                        HttpServletRequest httpRequest) {
        try {
            DepenseGeneraleResponse depense = comptabiliteService.enregistrerDepenseGenerale(request, httpRequest);
            return ResponseEntity.ok(depense);
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
     * Endpoint pour lister les dépenses générales (réservé au rôle COMPTABLE)
     */
    @GetMapping("/comptabilite/depenses-generales")
    public ResponseEntity<?> listerDepensesGenerales(HttpServletRequest httpRequest) {
        try {
            List<DepenseGeneraleResponse> depenses = comptabiliteService.listerDepensesGenerales(httpRequest);
            return ResponseEntity.ok(depenses);
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

