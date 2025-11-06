package com.xpertcash.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.xpertcash.entity.Client;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.service.ClientService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class ClientController {

    @Autowired
    private ClientService clientService;


    @PostMapping("/clients")
    public ResponseEntity<?> createClient(@RequestBody Client client, HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        try {
            // Sauvegarder le client avec son entreprise (si elle est associée)
            Client savedClient = clientService.saveClient(client, request);
            response.put("message", "Client créé avec succès");
            response.put("clientId", savedClient.getId().toString());
            response.put("createdAt", savedClient.getCreatedAt().toString());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // Gestion des erreurs pour les doublons de client ou entreprise
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }



    @GetMapping("/clients/{id}")
    public Optional<Client> getClientById(@PathVariable Long id) {
        return clientService.getClientById(id);
    }
    
    @GetMapping("/clients/{id}/interactions")
    public ResponseEntity<?> getClientInteractions(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<com.xpertcash.DTOs.PROSPECT.InteractionDTO> interactions = clientService.getClientInteractionDTOs(id);
            response.put("clientId", id);
            response.put("interactions", interactions);
            response.put("total", interactions.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Erreur lors de la récupération des interactions: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/clients")
    public ResponseEntity<?> getAllClients(HttpServletRequest request) {
        try {
            List<Client> clients = clientService.getAllClients(request);
            return ResponseEntity.ok(clients);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }


    @GetMapping("/clients/entreprise/{entrepriseId}")
    public List<Client> getClientsByEntreprise(@PathVariable Long entrepriseId) {
        return clientService.getClientsByEntreprise(entrepriseId);
    }

    @GetMapping("/clients-and-entreprises")
    public List<Object> getAllClientsAndEntreprises() {
        return clientService.getAllClientsAndEntreprises();
    }

    //Endpoint pour modifier un client 
    @PutMapping(value ="/clientupdate/{id}",  consumes = MediaType.MULTIPART_FORM_DATA_VALUE) 
    public ResponseEntity<?> updateClient(@PathVariable("id") Long id,
                                          @RequestPart("client") Client client,
                                          @RequestPart(value = "imageClientFile", required = false) MultipartFile imageClientFile,
                                          HttpServletRequest request) {
        try {
            client.setId(id);
            Client updatedClient = clientService.updateClient(client, imageClientFile, request);
            return ResponseEntity.ok(updatedClient);
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (EntityNotFoundException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (RuntimeException e) {
            return buildErrorResponse(HttpStatus.CONFLICT, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue s'est produite : " + e.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("message", message);
        return ResponseEntity.status(status).body(errorBody);
    }

    //Get entreprise client
    @GetMapping("/entreprise-clients")
    public ResponseEntity<List<EntrepriseClient>> getEntrepriseClients(HttpServletRequest request) {
        List<EntrepriseClient> entrepriseClients = clientService.getAllEntrepriseClients(request);
        return ResponseEntity.ok(entrepriseClients);
    }

    // Endpoint pour supprimer un client deleteClientIfNoOrdersOrInvoices
    @DeleteMapping("/clients/{id}")
    public ResponseEntity<?> deleteClientIfNoOrdersOrInvoices(@PathVariable Long id, HttpServletRequest request) {
        try {
            clientService.deleteClientIfNoOrdersOrInvoices(id, request);
            return ResponseEntity.ok().body("Client supprimé avec succès.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la suppression du client.");
        }
    }



}