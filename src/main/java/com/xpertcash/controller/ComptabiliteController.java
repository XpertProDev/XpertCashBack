package com.xpertcash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpertcash.DTOs.DepenseGeneraleRequestDTO;
import com.xpertcash.DTOs.EntreeGeneraleRequestDTO;
import com.xpertcash.DTOs.PayerDetteRequest;
import com.xpertcash.service.ComptabiliteService;
import com.xpertcash.service.IMAGES.ImageStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ComptabiliteController {

    @Autowired
    private ComptabiliteService comptabiliteService;

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private ObjectMapper objectMapper;

     // Endpoint qui retourne toutes les données comptables de l'entreprise

    @GetMapping("/comptabilite")
    public ResponseEntity<?> getComptabilite(HttpServletRequest request) {
        return handleRequest(() -> comptabiliteService.getComptabilite(request));
    }

     // Crée une dépense générale pour l'entreprise.
    
    @PostMapping(value = "/create/depenses-generales", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> creerDepenseGenerale(
            @RequestParam("depense") String depenseJson,
            @RequestParam(value = "pieceJointe", required = false) MultipartFile pieceJointeFile,
            HttpServletRequest httpRequest) {
        return handleRequest(() -> {
            DepenseGeneraleRequestDTO request = parseDepenseJson(depenseJson);
            String pieceJointeUrl = savePieceJointe(pieceJointeFile);
            request.setPieceJointe(pieceJointeUrl);
            return comptabiliteService.creerDepenseGenerale(request, httpRequest);
        });
    }

    @GetMapping("/list/depenses-generales")
    public ResponseEntity<?> listerDepensesGenerales(HttpServletRequest request) {
        return handleRequest(() -> comptabiliteService.listerDepensesGenerales(request));
    }

    @PostMapping("/create/categories-depense")
    public ResponseEntity<?> creerCategorieDepense(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        return handleRequest(() -> {
            String nom = request.get("nom");
            String description = request.get("description");
            return comptabiliteService.creerCategorieDepense(nom, description, httpRequest);
        });
    }

    @GetMapping("/list/categories-depense")
    public ResponseEntity<?> listerCategoriesDepense(HttpServletRequest request) {
        return handleRequest(() -> comptabiliteService.listerCategoriesDepense(request));
    }

    @PostMapping("/create/categories-entree")
    public ResponseEntity<?> creerCategorieEntree(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        return handleRequest(() -> {
            String nom = request.get("nom");
            String description = request.get("description");
            return comptabiliteService.creerCategorieEntree(nom, description, httpRequest);
        });
    }

    // @GetMapping("/list/categories-entree")
    // public ResponseEntity<?> listerCategoriesEntree(HttpServletRequest request) {
    //     return handleRequest(() -> comptabiliteService.listerCategoriesEntree(request));
    // }


     // Crée une entrée générale pour l'entreprise.
    @PostMapping(value = "/create/entrees-generales", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public ResponseEntity<?> creerEntreeGenerale(
            @RequestParam("entree") String entreeJson,
            @RequestParam(value = "pieceJointe", required = false) MultipartFile pieceJointeFile,
            HttpServletRequest httpRequest) {
        return handleRequest(() -> {
            EntreeGeneraleRequestDTO request = parseEntreeJson(entreeJson);
            String pieceJointeUrl = savePieceJointe(pieceJointeFile);
            request.setPieceJointe(pieceJointeUrl);
            return comptabiliteService.creerEntreeGenerale(request, httpRequest);
        });
    }

    @GetMapping("/list/entrees-generales")
    public ResponseEntity<?> listerEntreesGenerales(HttpServletRequest request) {
        return handleRequest(() -> comptabiliteService.listerEntreesGenerales(request));
    }

    /** Comptabilité complète paginée, avec filtre de période (même paramètres que GET /tresorerie). */
    @GetMapping("/comptabilite/complete")
    public ResponseEntity<?> getComptabiliteComplete(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "aujourdhui") String periode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
            HttpServletRequest request) {
        return handleRequest(() -> comptabiliteService.getComptabiliteCompletePaginated(request, page, size, periode, dateDebut, dateFin));
    }
    

     // Payer une dette depuis la comptabilité (actuellement pour VENTE_CREDIT).
    @PostMapping("/comptabilite/dettes/payer")
    public ResponseEntity<?> payerDette(@RequestBody PayerDetteRequest request, HttpServletRequest httpRequest) {
        return handleRequest(() -> {
            comptabiliteService.payerDette(request, httpRequest);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Dette payée avec succès");
            return response;
        });
    }


     // Gère les requêtes avec gestion d'erreur centralisée
    
    private ResponseEntity<?> handleRequest(java.util.function.Supplier<Object> supplier) {
        try {
            return ResponseEntity.ok(supplier.get());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("Erreur interne du serveur : " + e.getMessage()));
        }
    }

     // Crée une réponse d'erreur
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

     // Parse le JSON de dépense et corrige les problèmes de format
    private DepenseGeneraleRequestDTO parseDepenseJson(String depenseJson) {
        try {
            String cleanedJson = cleanJson(depenseJson);
            return objectMapper.readValue(cleanedJson, DepenseGeneraleRequestDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Format JSON invalide : " + e.getMessage(), e);
        }
    }

     // Nettoie et corrige le JSON si nécessaire (ajoute les accolades manquantes)
    private String cleanJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new RuntimeException("Le JSON de dépense est vide");
        }
        
        String cleaned = json.trim();
        
        if (!cleaned.startsWith("{")) {
            cleaned = "{" + cleaned;
        }
        
        if (!cleaned.endsWith("}")) {
            cleaned = cleaned + "}";
        }
        
        return cleaned;
    }

     // Sauvegarde la pièce jointe si elle est présente
    private String savePieceJointe(MultipartFile pieceJointeFile) {
        if (pieceJointeFile != null && !pieceJointeFile.isEmpty()) {
            return imageStorageService.saveDepensePieceJointe(pieceJointeFile);
        }
        return null;
    }

     // Parse le JSON d'entrée et corrige les problèmes de format
    private EntreeGeneraleRequestDTO parseEntreeJson(String entreeJson) {
        try {
            String cleanedJson = cleanJson(entreeJson);
            return objectMapper.readValue(cleanedJson, EntreeGeneraleRequestDTO.class);
        } catch (Exception e) {
            throw new RuntimeException("Format JSON invalide : " + e.getMessage(), e);
        }
    }
}

