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

import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.service.EntrepriseClientService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class EntrepriseClientController {

    @Autowired
    private EntrepriseClientService entrepriseClientService;

    @PostMapping("/entreprise-clients")
    public ResponseEntity<?> createEntrepriseClient(@RequestBody EntrepriseClient entrepriseClient, HttpServletRequest request) {
        try {
            EntrepriseClient saved = entrepriseClientService.saveEntreprise(entrepriseClient, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de l'enregistrement de l'entreprise cliente.");
        }
    }


    @GetMapping("/entreprises/{id}")
    public ResponseEntity<?> getEntrepriseById(@PathVariable Long id, HttpServletRequest request) {
        Optional<EntrepriseClient> entrepriseClient = entrepriseClientService.getEntrepriseById(id, request);

        if (entrepriseClient.isPresent()) {
            return ResponseEntity.ok(entrepriseClient.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Aucune entreprise cliente trouvée avec l'ID : " + id);
        }
    }

 

   @GetMapping("/entreprises")
    public ResponseEntity<?> getAllEntreprises(HttpServletRequest request) {
        List<EntrepriseClient> entreprises = entrepriseClientService.getAllEntreprises(request);

        if (entreprises.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Aucune entreprise cliente trouvée.");
        }

        return ResponseEntity.ok(entreprises);
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

    
    @DeleteMapping("/entrepriseClients/{id}")
    public ResponseEntity<?> deleteEntrepriseClientIfNoOrdersOrInvoices(@PathVariable Long id, HttpServletRequest request) {
        try {
            entrepriseClientService.deleteEntrepriseClientIfNoOrdersOrInvoices(id, request);
            return ResponseEntity.ok().body("Client entreprise supprimé avec succès.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la suppression du client entreprise.");
        }
    }

}
