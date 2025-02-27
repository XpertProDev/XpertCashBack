package com.xpertcash.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.entity.Stock;
import com.xpertcash.service.StockService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class StockController {
     @Autowired
    private StockService stockService;

    // Ajouter un stock à une boutique (seul l'admin peut le faire)
    @PostMapping("/ajouterStock")
    public ResponseEntity<Stock> ajouterStock(
            HttpServletRequest request,
            @RequestParam Long boutiqueId,
            @RequestParam Long produitId,
            @RequestParam int quantite) {
        
        // Utilisation du service pour ajouter du stock
        Stock stock = stockService.ajouterStock(request, boutiqueId, produitId, quantite);

        // Retourner le stock créé avec un statut CREATED (201)
        return ResponseEntity.status(HttpStatus.CREATED).body(stock);
    }

    // Récupérer les stocks associés à une boutique
    @GetMapping("/boutique/{boutiqueId}")
    public ResponseEntity<List<Stock>> getStocksByBoutique(
            @PathVariable Long boutiqueId) {

        // Utilisation du service pour récupérer les stocks de la boutique
        List<Stock> stocks = stockService.getStocksByBoutique(boutiqueId);

        // Retourner la liste des stocks
        return ResponseEntity.ok(stocks);
    }

}
