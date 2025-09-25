package com.xpertcash.controller.VENTE;

import com.xpertcash.DTOs.VENTE.TransactionSummaryDTO;
import com.xpertcash.service.VENTE.TransactionSummaryService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class TransactionSummaryController {

    @Autowired
    private TransactionSummaryService transactionSummaryService;

    /**
     * Récupère le résumé complet de toutes les transactions financières
     * pour l'entreprise de l'utilisateur connecté (année en cours)
     */
    @GetMapping("/transactions/resume-complet")
    public ResponseEntity<?> getTransactionSummary(HttpServletRequest request) {
        try {
            TransactionSummaryDTO summary = transactionSummaryService.getTransactionSummary(request);
            return ResponseEntity.ok(summary);
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
     * Récupère le résumé des transactions du jour
     */
    @GetMapping("/transactions/resume-jour")
    public ResponseEntity<?> getTransactionSummaryDuJour(HttpServletRequest request) {
        try {
            TransactionSummaryDTO summary = transactionSummaryService.getTransactionSummaryDuJour(request);
            return ResponseEntity.ok(summary);
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
     * Récupère le résumé des transactions du mois
     */
    @GetMapping("/transactions/resume-mois")
    public ResponseEntity<?> getTransactionSummaryDuMois(HttpServletRequest request) {
        try {
            TransactionSummaryDTO summary = transactionSummaryService.getTransactionSummaryDuMois(request);
            return ResponseEntity.ok(summary);
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
     * Récupère le résumé des transactions pour une période personnalisée
     * Format des dates : yyyy-MM-dd HH:mm:ss
     */
    @GetMapping("/transactions/resume-periode")
    public ResponseEntity<?> getTransactionSummaryPeriode(
            @RequestParam String dateDebut,
            @RequestParam String dateFin,
            HttpServletRequest request) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime debut = LocalDateTime.parse(dateDebut, formatter);
            LocalDateTime fin = LocalDateTime.parse(dateFin, formatter);
            
            // Validation des dates
            if (debut.isAfter(fin)) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "La date de début ne peut pas être postérieure à la date de fin");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Récupérer l'entreprise de l'utilisateur connecté
            // Note: Vous devrez adapter cette partie selon votre logique d'authentification
            // Pour l'instant, on utilise la méthode existante qui récupère l'entreprise
            
            TransactionSummaryDTO summary = transactionSummaryService.getTransactionSummary(request);
            // TODO: Adapter pour utiliser les dates personnalisées
            // TransactionSummaryDTO summary = transactionSummaryService.getTransactionSummary(entrepriseId, debut, fin);
            
            return ResponseEntity.ok(summary);
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
     * Récupère uniquement les totaux (sans le détail des transactions)
     * Plus rapide pour les tableaux de bord
     */
    @GetMapping("/transactions/totaux")
    public ResponseEntity<?> getTotauxTransactions(HttpServletRequest request) {
        try {
            TransactionSummaryDTO summary = transactionSummaryService.getTransactionSummary(request);
            
            // Créer une réponse simplifiée avec seulement les totaux
            Map<String, Object> totaux = new HashMap<>();
            totaux.put("totalEntrees", summary.getTotalEntrees());
            totaux.put("totalSorties", summary.getTotalSorties());
            totaux.put("soldeNet", summary.getSoldeNet());
            totaux.put("totalVentes", summary.getTotalVentes());
            totaux.put("totalPaiementsFactures", summary.getTotalPaiementsFactures());
            totaux.put("totalRemboursements", summary.getTotalRemboursements());
            totaux.put("totalDepenses", summary.getTotalDepenses());
            totaux.put("periode", summary.getPeriode());
            totaux.put("nombreTransactions", summary.getTransactions().size());
            
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
