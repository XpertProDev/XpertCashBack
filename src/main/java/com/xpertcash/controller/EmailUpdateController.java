package com.xpertcash.controller;

import com.xpertcash.DTOs.EmailUpdateConfirmationRequest;
import com.xpertcash.DTOs.EmailUpdateRequest;
import com.xpertcash.service.EmailUpdateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class EmailUpdateController {

    @Autowired
    private EmailUpdateService emailUpdateService;

    /**
     * Endpoint pour initier la demande de changement d'email.
     * Le code de vérification est envoyé à l'email actuel de l'utilisateur.
     */
    @PostMapping("/email-update/{userId}")
    public ResponseEntity<String> requestEmailUpdate(@PathVariable Long userId, @Valid @RequestBody EmailUpdateRequest request) {
        try {
            emailUpdateService.requestEmailUpdate(userId, request);
            return ResponseEntity.ok("Un code de vérification a été envoyé à votre email actuel.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la demande de modification d'email : " + e.getMessage());
        }
    }

    /**
     * Endpoint pour confirmer le changement d'email avec le code de vérification.
     */
    @PostMapping("/confirm-email/{userId}")
    public ResponseEntity<String> confirmEmailUpdate(@PathVariable Long userId, @Valid @RequestBody EmailUpdateConfirmationRequest request) {
        try {
            emailUpdateService.confirmEmailUpdate(userId, request);
            return ResponseEntity.ok("Votre email a été mis à jour avec succès.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la confirmation de l'email : " + e.getMessage());
        }
    }
}
