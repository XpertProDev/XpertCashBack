package com.xpertcash.controller.PASSWORD;

import com.xpertcash.DTOs.PASSWORD.PasswordUpdateConfirmationRequest;
import com.xpertcash.DTOs.PASSWORD.PasswordUpdateRequest;
import com.xpertcash.service.PASSWORD.PasswordUpdateService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class PasswordUpdateController {

    @Autowired
    private PasswordUpdateService passwordUpdateService;

    /**
     * Endpoint pour initier la demande de changement de mot de passe.
     * L'utilisateur fournit son mot de passe actuel et le nouveau mot de passe.
     * Un code de vérification est envoyé à son email actuel.
     */
    @PutMapping("/update-password/{userId}")
    public ResponseEntity<String> requestPasswordUpdate(@PathVariable Long userId, @Valid @RequestBody PasswordUpdateRequest request) {
        try {
            passwordUpdateService.requestPasswordUpdate(userId, request);
            return ResponseEntity.ok("Un code de vérification a été envoyé à votre email actuel.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la demande de modification de mot de passe : " + e.getMessage());
        }
    }

    /**
     * Endpoint pour confirmer le changement de mot de passe avec le code de vérification.
     */
    @PostMapping("/confirm-password/{userId}")
    public ResponseEntity<String> confirmPasswordUpdate(@PathVariable Long userId, @Valid @RequestBody PasswordUpdateConfirmationRequest request) {
        try {
            passwordUpdateService.confirmPasswordUpdate(userId, request);
            return ResponseEntity.ok("Votre mot de passe a été mis à jour avec succès.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la confirmation du mot de passe : " + e.getMessage());
        }
    }
}
