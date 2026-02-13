package com.xpertcash.controller;

import com.xpertcash.service.TresorerieService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/auth")
public class TresorerieController {

    @Autowired
    private TresorerieService tresorerieService;

    @GetMapping("/tresorerie")
    public ResponseEntity<?> getTresorerie(
            HttpServletRequest request,
            @RequestParam(required = false, defaultValue = "aujourdhui") String periode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return handleRequest(() -> tresorerieService.calculerTresorerie(request, periode, dateDebut, dateFin));
    }

    @GetMapping("/tresorerie/dettes")
    public ResponseEntity<?> getDettesDetaillees(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return handleRequest(() -> tresorerieService.getDettesDetaillees(request, page, size));
    }

     // Dettes issues uniquement du POS (ventes à crédit).
   
    @GetMapping("/tresorerie/dettes/pos")
    public ResponseEntity<?> getDettesPos(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "aujourdhui") String periode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long vendeurId,
            @RequestParam(required = false) Long boutiqueId) {
        return handleRequest(() -> tresorerieService.getDettesPos(request, page, size,
                periode, dateDebut, dateFin, sortBy, sortDir, vendeurId, boutiqueId));
    }

    private ResponseEntity<?> handleRequest(Supplier<Object> supplier) {
        try {
            return ResponseEntity.ok(supplier.get());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(createErrorResponse("Erreur interne du serveur : " + e.getMessage()));
        }
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
