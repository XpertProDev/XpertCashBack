package com.xpertcash.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import com.xpertcash.DTOs.EntrepriseDTO;
import com.xpertcash.service.EntrepriseService;

@RestController
@RequestMapping("/api/auth")
public class EntrepriseController {

     @Autowired
    private EntrepriseService entrepriseService;

    @GetMapping("/allentreprises")
    public ResponseEntity<?> getAllEntreprises() {
        try {
            List<EntrepriseDTO> entreprises = entrepriseService.getAllEntreprisesWithInfo();
            return ResponseEntity.ok(entreprises);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Erreur interne : " + e.getMessage()));
        }
    }

}
