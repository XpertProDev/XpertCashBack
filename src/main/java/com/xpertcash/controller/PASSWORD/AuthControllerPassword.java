package com.xpertcash.controller.PASSWORD;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.service.PASSWORD.PasswordService;

@RestController
@RequestMapping("/api/auth")
public class AuthControllerPassword {

     @Autowired
    private PasswordService passwordService;

        // Étape 1 : Demander un code de réinitialisation
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            passwordService.generateResetToken(email);
            return ResponseEntity.ok(Collections.singletonMap("message", "Un code de vérification a été envoyé à votre email."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        }
    }


        // Étape 2 : Vérifier le code OTP
        @PostMapping("/verify-reset-token")
        public ResponseEntity<?> verifyToken(@RequestBody Map<String, String> request) {
            try {
                String email = request.get("email");
                String token = request.get("token");
    
                if (passwordService.verifyResetToken(email, token)) {
                    return ResponseEntity.ok(Collections.singletonMap("message", "Code valide, vous pouvez maintenant changer votre mot de passe."));
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", "Code invalide ou expiré."));
                }
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
            }
        }


            // Étape 3 : Modifier le mot de passe
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String token = request.get("token");
            String newPassword = request.get("newPassword");

            passwordService.resetPassword(email, token, newPassword);
            return ResponseEntity.ok(Collections.singletonMap("message", "Mot de passe changé avec succès."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

}
