package com.xpertcash.controller;

import com.xpertcash.DTOs.ComptabiliteDTO;
import com.xpertcash.service.ComptabiliteService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
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
}

