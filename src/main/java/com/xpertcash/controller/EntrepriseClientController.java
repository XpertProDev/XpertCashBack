package com.xpertcash.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.entity.Client;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.service.EntrepriseClientService;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/auth")
public class EntrepriseClientController {

    @Autowired
    private EntrepriseClientService entrepriseClientService;

    @PostMapping("/entreprises")
    public ResponseEntity<?> createEntreprise(@RequestBody EntrepriseClient entrepriseClient) {
        try {
            EntrepriseClient savedEntreprise = entrepriseClientService.saveEntreprise(entrepriseClient);
            return ResponseEntity.ok(savedEntreprise);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/entreprises/{id}")
    public Optional<EntrepriseClient> getEntrepriseById(@PathVariable Long id) {
        return entrepriseClientService.getEntrepriseById(id);
    }

    @GetMapping("/entreprises")
    public List<EntrepriseClient> getAllEntreprises() {
        return entrepriseClientService.getAllEntreprises();
    }

    @DeleteMapping("/entreprises/{id}")
    public void deleteEntreprise(@PathVariable Long id) {
        entrepriseClientService.deleteEntreprise(id);
    }



    //Endpoint pour modifier une Entreprise client
     @PutMapping("/cliententrepriseupdate/{id}")
    public ResponseEntity<EntrepriseClient> updateEntrepriseClient(@PathVariable("id") Long id, @RequestBody EntrepriseClient entrepriseClient) {
        try {
            entrepriseClient.setId(id);
            EntrepriseClient updateEntrepriseClient = entrepriseClientService.updateEntrepriseClient(entrepriseClient);
            return ResponseEntity.ok(updateEntrepriseClient);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
