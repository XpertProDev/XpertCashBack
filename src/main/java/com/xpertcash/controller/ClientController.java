package com.xpertcash.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xpertcash.entity.Client;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.service.ClientService;

@RestController
@RequestMapping("/api/auth")
public class ClientController {

    @Autowired
    private ClientService clientService;

  
        @PostMapping("/clients")
    public ResponseEntity<?> createClient(@RequestBody Client client) {
        Map<String, String> response = new HashMap<>();
        try {
            // Sauvegarder le client avec son entreprise (si elle est associée)
            Client savedClient = clientService.saveClient(client);
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

   @GetMapping("/clients")
    public List<Client> getAllClients() {
        return clientService.getAllClients();
    }

    @GetMapping("/clients/entreprise/{entrepriseId}")
    public List<Client> getClientsByEntreprise(@PathVariable Long entrepriseId) {
        return clientService.getClientsByEntreprise(entrepriseId);
    }

    @DeleteMapping("/clients/{id}")
    public void deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
    }


    @GetMapping("/clients-and-entreprises")
    public List<Object> getAllClientsAndEntreprises() {
        return clientService.getAllClientsAndEntreprises();
    }
}
