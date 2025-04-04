package com.xpertcash.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;

    @PostMapping("/clients")
    public Client createClient(@RequestBody Client client) {
        // Si une entreprise est fournie, vérifie si l'entreprise a un ID
        if (client.getEntrepriseClient() != null) {
            // Si l'entreprise n'a pas d'ID (nouvelle entreprise)
            if (client.getEntrepriseClient().getId() == null) {
                // Sauvegarder l'entreprise avant de l'associer au client
                EntrepriseClient savedEntreprise = entrepriseClientRepository.save(client.getEntrepriseClient());
                // Associer l'entreprise nouvellement créée au client
                client.setEntrepriseClient(savedEntreprise);
            }
        }

        // Sauvegarder le client avec son entreprise (si elle est associée)
        return clientService.saveClient(client);
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
