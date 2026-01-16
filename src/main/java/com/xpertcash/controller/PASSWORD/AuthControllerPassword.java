package com.xpertcash.controller.PASSWORD;

import java.util.Collections;
import java.util.Map;

import com.xpertcash.entity.PASSWORD.PasswordResetToken;
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

        // Demander un code de réinitialisation
        @PostMapping("/forgot-password")
        public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
            String email = request.get("email");
            try {
                passwordService.generateResetToken(email);
                return ResponseEntity.ok(Collections.singletonMap("message", "Un code de vérification a été envoyé à votre email."));
            } catch (Exception e) {
                // Affiche le vrai message d'erreur côté client pour debug
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erreur : " + e.getMessage()));
            }
        }

        


        //  Modifier le mot de passe
        @PostMapping("/reset-password")
        public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
            try {
                String token = request.get("token");
                String newPassword = request.get("newPassword");

                passwordService.resetPassword(token, newPassword);
                return ResponseEntity.ok(Collections.singletonMap("message", "Mot de passe changé avec succès."));
            } catch (RuntimeException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Collections.singletonMap("error", e.getMessage()));
            }
        }

    @PostMapping("/validate-otp")
    public ResponseEntity<?> validateOtp(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String code = request.get("code");

        try {
            PasswordResetToken token = passwordService.validateOtp(email, code);
            return ResponseEntity.ok(Collections.singletonMap("token", token.getToken()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }


}
