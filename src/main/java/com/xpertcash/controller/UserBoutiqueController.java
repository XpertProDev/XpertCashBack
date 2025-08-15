package com.xpertcash.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.DTOs.Boutique.AssignerVendeurRequest;
import com.xpertcash.DTOs.Boutique.VendeurDTO;
import com.xpertcash.service.UserBoutiqueService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class UserBoutiqueController {

    @Autowired
    private UserBoutiqueService userBoutiqueService;

       // Endpoint pour assigner un utilisateur à plusieurs boutiques
    @PostMapping("/assigner-vendeur")
    public ResponseEntity<Map<String, Object>> assignerVendeur(@RequestBody AssignerVendeurRequest request, HttpServletRequest httpServletRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> resultMessages = userBoutiqueService.assignerVendeurAuxBoutiques(httpServletRequest, request.getUserId(), request.getBoutiqueIds());

            // Si aucune affectation n'a eu lieu
            if (resultMessages.isEmpty()) {
                response.put("message", "Aucune boutique n'a été affectée.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // En cas de succès ou de boutiques déjà affectées
            response.put("status", "success");
            response.put("messages", resultMessages);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            e.printStackTrace(); // voir dans la console l'origine exacte
            response.put("status", "error");
            response.put("message", e.getMessage() != null ? e.getMessage() : e.toString());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

    }


    

    // Endpoint pour supprimer l'affectation d'un utilisateur à une boutique spécifique
   @PostMapping("/retirer-vendeur")
    public ResponseEntity<Map<String, Object>> retirerVendeur(@RequestBody AssignerVendeurRequest request, HttpServletRequest httpServletRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> resultMessages = userBoutiqueService.retirerVendeurDesBoutiques(httpServletRequest, request.getUserId(), request.getBoutiqueIds());

            if (resultMessages.isEmpty()) {
                response.put("message", "Aucune boutique n'a été affectée.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            response.put("status", "success");
            response.put("messages", resultMessages);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


        // Endpoint pour récupérer toutes les boutiques d'un utilisateur
        @GetMapping("/boutiques/{userId}")
            public ResponseEntity<Map<String, Object>> getBoutiquesParUtilisateur(@PathVariable Long userId, 
            HttpServletRequest request) {
            return userBoutiqueService.getBoutiquesParUtilisateur(userId, request);
        }

    // Récupérer les vendeurs d'une boutique
    @GetMapping("/vendeurs/{boutiqueId}")
    public ResponseEntity<List<VendeurDTO>> getVendeursDeBoutique(
            @PathVariable Long boutiqueId,
            HttpServletRequest request) {
        List<VendeurDTO> vendeursDTO = userBoutiqueService.getVendeursDeBoutique(boutiqueId, request);
        return ResponseEntity.ok(vendeursDTO);
    }

}
