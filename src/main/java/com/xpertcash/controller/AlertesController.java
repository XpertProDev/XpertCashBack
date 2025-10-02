package com.xpertcash.controller;

import com.xpertcash.DTOs.AlerteStockDTO;
import com.xpertcash.service.AlertesService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AlertesController {

    @Autowired
    private AlertesService alertesService;

    /**
     * Endpoint pour récupérer les alertes de stock faible
     * Retourne les produits dont le stock actuel est inférieur ou égal au seuil d'alerte
     */
    @GetMapping("/alertes/stock-faible")
    public ResponseEntity<?> getAlertesStockFaible(HttpServletRequest request) {
        try {
            List<AlerteStockDTO> alertes = alertesService.getAlertesStockFaible(request);
            return ResponseEntity.ok(alertes);
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

