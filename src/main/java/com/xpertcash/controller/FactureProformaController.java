package com.xpertcash.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.composant.AuthorizationService;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.StatutFactureProForma;
import com.xpertcash.service.FactureProformaService;
import com.xpertcash.service.UsersService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class FactureProformaController {

      @Autowired
    private FactureProformaService factureProformaService;

      @Autowired
    private UsersService usersService;
    @Autowired
    private JwtUtil jwtUtil;


    // Endpoint pour ajouter une facture pro forma
    @PostMapping("/ajouter")
    public ResponseEntity<?> ajouterFacture(
            @RequestBody FactureProForma facture,
            @RequestParam(defaultValue = "0") Double remisePourcentage,
            @RequestParam(defaultValue = "false") Boolean appliquerTVA,
            @RequestHeader("Authorization") String token,  // Récupération du token depuis l'en-tête
            HttpServletRequest request) {  // Passage du HttpServletRequest complet

        try {
            // Ajouter le token dans l'en-tête de la requête
            request.setAttribute("Authorization", token);

            // Appel du service pour ajouter la facture, en passant la requête avec le token
            FactureProForma nouvelleFacture = factureProformaService.ajouterFacture(facture, remisePourcentage, appliquerTVA, request);

            // Retourner la facture créée en réponse HTTP 201 (CREATED)
            return ResponseEntity.status(HttpStatus.CREATED).body(nouvelleFacture);
        } catch (RuntimeException e) {
            // Retourner l'erreur en réponse HTTP 400 (BAD REQUEST) si une exception est levée
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    
    

    // Endpoint pour modifier une facture pro forma
    @PutMapping("/updatefacture/{factureId}")
    public ResponseEntity<FactureProForma> updateFacture(
            @PathVariable Long factureId,
            @RequestParam(required = false) Double remisePourcentage,
            @RequestParam(required = false) Boolean appliquerTVA,
            @RequestBody FactureProForma modifications, HttpServletRequest request) {
    
        FactureProForma factureModifiee = factureProformaService.modifierFacture(factureId, remisePourcentage, appliquerTVA, modifications, request);
        return ResponseEntity.ok(factureModifiee);
    }

    // Endpoint pour recuperer la liste des factures pro forma dune entreprise
    @GetMapping("/mes-factures")
    public ResponseEntity<List<Map<String, Object>>> getFacturesParEntreprise(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    
        Long userId;
        try {
            userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    
        List<Map<String, Object>> factures = factureProformaService.getFacturesParEntreprise(userId);
    
        return ResponseEntity.ok(factures);
    }

    // Endpoint Get bye id
    @GetMapping("/factureProforma/{id}")
    public ResponseEntity<FactureProForma> getFactureProformaById(@PathVariable Long id, HttpServletRequest request) {
        FactureProForma facture = factureProformaService.getFactureProformaById(id, request);
        return ResponseEntity.ok(facture);
    }
    
    


}
