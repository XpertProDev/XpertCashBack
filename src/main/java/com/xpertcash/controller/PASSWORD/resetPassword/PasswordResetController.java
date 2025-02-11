package com.xpertcash.controller.PASSWORD.resetPassword;


import com.xpertcash.DTOs.PASSWORD.resetPassword.PasswordResetConfirmation;
import com.xpertcash.DTOs.PASSWORD.resetPassword.PasswordResetRequest;
import com.xpertcash.service.PASSWORD.resetPassword.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * Endpoint pour initier la réinitialisation du mot de passe.
     * L'utilisateur fournit son email, et un code de vérification est envoyé à cet email.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        try {
            passwordResetService.requestPasswordReset(request);
            return ResponseEntity.ok("Un code de vérification a été envoyé à votre email.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la demande de réinitialisation : " + e.getMessage());
        }
    }

    /**
     * Endpoint pour confirmer la réinitialisation du mot de passe.
     * L'utilisateur fournit son email, le code de vérification reçu et son nouveau mot de passe.
     */
    @PostMapping("/confirm-reset-password")
    public ResponseEntity<String> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmation confirmation) {
        try {
            passwordResetService.confirmPasswordReset(confirmation);
            return ResponseEntity.ok("Votre mot de passe a été réinitialisé avec succès.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la confirmation de réinitialisation : " + e.getMessage());
        }
    }
}
