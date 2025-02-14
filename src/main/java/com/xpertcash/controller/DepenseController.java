package com.xpertcash.controller;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.entity.Depense;
import com.xpertcash.entity.User;
import com.xpertcash.exceptions.NotFoundException;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.DepenseService;

@RestController

@RequestMapping("/api/depenses")
public class DepenseController {

    @Autowired
    private DepenseService depenseService;

    @Autowired
    private UsersRepository usersRepository;

    // Endpoint pour créer une nouvelle dépense (approvisionnement)
    @PostMapping("/ajouter")
    public ResponseEntity<?> ajouterDepense(
            @RequestParam Long userId, // Passer un userId au lieu de User
            @RequestParam double montant,
            @RequestParam String description) {
        try {
            // Vérifier que l'utilisateur existe
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));
    
            // Enregistrer la dépense
            Depense depense = depenseService.enregistrerDepense(montant, description, user); // Passer user au lieu de userId
    
            return ResponseEntity.status(HttpStatus.CREATED).body(depense);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }
    
    // Endpoint pour récupérer toutes les dépenses
    @GetMapping("/liste")
    public ResponseEntity<?> getAllDepenses() {
        try {
            List<Depense> depenses = depenseService.getAllDepenses();
            return ResponseEntity.ok(depenses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }

    // Endpoint pour récupérer les dépenses par utilisateur
    @GetMapping("/utilisateur/{userId}")
    public ResponseEntity<?> getDepensesByUser(@PathVariable Long userId) {
        try {
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("Utilisateur non trouvé."));
            List<Depense> depenses = depenseService.getDepensesByUser(user);
            return ResponseEntity.ok(depenses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }

    // Endpoint pour récupérer les dépenses entre deux dates
    @GetMapping("/date-range")
    public ResponseEntity<?> getDepensesBetweenDates(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
                                                     @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        try {
            List<Depense> depenses = depenseService.getDepensesBetweenDates(startDate, endDate);
            return ResponseEntity.ok(depenses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }
}
