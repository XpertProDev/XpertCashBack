package com.xpertcash.controller;

import com.xpertcash.DTOs.ActiviteHebdoDTO;
import com.xpertcash.DTOs.StatistiquesGlobalesDTO;
import com.xpertcash.service.StatistiquesGlobalesService;
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
public class StatistiquesGlobalesController {

    @Autowired
    private StatistiquesGlobalesService statistiquesService;

     // Endpoint unique qui retourne TOUTES les statistiques globales de l'entreprise
  
    @GetMapping("/statistiques/globales")
    public ResponseEntity<?> getStatistiquesGlobales(HttpServletRequest request) {
        try {
            StatistiquesGlobalesDTO statistiques = statistiquesService.getStatistiquesGlobales(request);
            return ResponseEntity.ok(statistiques);
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

     // Endpoint qui retourne les statistiques d'activité hebdomadaire (7 derniers jours)
    @GetMapping("/statistiques/activite-hebdo")
    public ResponseEntity<?> getActiviteHebdomadaire(HttpServletRequest request) {
        try {
            ActiviteHebdoDTO activite = statistiquesService.getActiviteHebdomadaire(request);
            return ResponseEntity.ok(activite);
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

