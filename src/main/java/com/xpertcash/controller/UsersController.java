package com.xpertcash.controller;

import com.xpertcash.DTOs.UpdateUserRequest;
import com.xpertcash.DTOs.LoginRequest;
import com.xpertcash.DTOs.RegistrationRequest;
import com.xpertcash.service.UsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UsersController {

    @Autowired
    private UsersService usersService;

    // Inscription
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody RegistrationRequest request) {
        try {
            usersService.registerUsers(
                    request.getNomComplet(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getPhone(),
                    request.getNomEntreprise()
            );
            return ResponseEntity.ok("Compte créé avec succès. Un lien d'activation vous a été envoyé par email.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'inscription : " + e.getMessage());
        }
    }

    
    // Connexion
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        try {
            usersService.login(request.getEmail(), request.getPassword());
            return ResponseEntity.ok("Connexion réussie.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    // Activation du compte via le lien d'activation (GET avec paramètres dans l'URL)
    @GetMapping("/activate")
    public ResponseEntity<String> activate(@RequestParam("email") String email,
                                           @RequestParam("code") String code) {
        try {
            usersService.activateAccount(email, code);
            return ResponseEntity.ok("Compte activé avec succès.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Pour récupérer le statut du compte d'un utilisateur
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam("email") String email) {
        try {
            Map<String, Object> status = usersService.getAccountStatus(email);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Pour la mise en jour de user
    @PutMapping("/updateUsers/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        try {
            usersService.updateUser(id, request);
            return ResponseEntity.ok("Utilisateur mis à jour avec succès");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de la mise à jour : " + e.getMessage());
        }
    }

    // Déverrouillage du compte via le lien de déverrouillage (GET avec paramètres)
    /*@GetMapping("/unlock")
    public ResponseEntity<String> unlock(@RequestParam("email") String email,
                                         @RequestParam("code") String code) {
        try {
            usersService.unlockAccount(email, code);
            return ResponseEntity.ok("Compte déverrouillé avec succès.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }*/
}
