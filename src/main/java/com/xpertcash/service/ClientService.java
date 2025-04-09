package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.entity.Client;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;

  
    public Client saveClient(Client client) {
        // Vérifier si un client avec le même email ou téléphone existe déjà
        checkClientExists(client);
    
        // Vérifier et associer une entreprise si fournie
        if (client.getEntrepriseClient() != null) {
            // Vérifier l'existence de l'entreprise par email ou téléphone
            checkEntrepriseExists(client.getEntrepriseClient());
    
            // Si l'entreprise a un ID, l'associer au client, sinon, la créer
            if (client.getEntrepriseClient().getId() != null) {
                associateExistingEntreprise(client);
            } else {
                saveNewEntreprise(client);
            }
        }
    
        client.setCreatedAt(LocalDateTime.now());
        // Sauvegarder le client avec son entreprise (si elle est associée)
        return clientRepository.save(client);
    }
    
    private void checkClientExists(Client client) {
        Optional<Client> existingClient = clientRepository.findByEmailOrTelephone(client.getEmail(), client.getTelephone());
        if (existingClient.isPresent()) {
            throw new RuntimeException("Un client avec les mêmes informations existe déjà !");
        }
    }
    
    private void checkEntrepriseExists(EntrepriseClient entrepriseClient) {
        Optional<EntrepriseClient> existingEntreprise = entrepriseClientRepository.findByEmailOrTelephone(entrepriseClient.getEmail(), entrepriseClient.getTelephone());
        if (existingEntreprise.isPresent()) {
            throw new RuntimeException("Cette entreprise existe déjà avec les mêmes coordonnées !");
        }
    }
    
    private void associateExistingEntreprise(Client client) {
        Optional<EntrepriseClient> existingEntrepriseById = entrepriseClientRepository.findById(client.getEntrepriseClient().getId());
        if (existingEntrepriseById.isPresent()) {
            client.setEntrepriseClient(existingEntrepriseById.get());
        } else {
            throw new IllegalArgumentException("L'entreprise avec cet ID n'existe pas.");
        }
    }
    
    private void saveNewEntreprise(Client client) {
        EntrepriseClient savedEntreprise = entrepriseClientRepository.save(client.getEntrepriseClient());
        client.setEntrepriseClient(savedEntreprise);
    }
    

    public Optional<Client> getClientById(Long id) {
        return clientRepository.findById(id);
    }

    public List<Client> getClientsByEntreprise(Long entrepriseId) {
        return clientRepository.findByEntrepriseClientId(entrepriseId);
    }

    public void deleteClient(Long id) {
        clientRepository.deleteById(id);
    }


    public List<Client> getAllClients() {
        List<Client> clients = clientRepository.findAll();
        List<EntrepriseClient> entreprises = entrepriseClientRepository.findAll();

        
        return clients; 
    }

    
      // Méthode pour récupérer tous les clients (personnes) et entreprises sans leurs clients associés
      public List<Object> getAllClientsAndEntreprises() {
        List<Object> clientsAndEntreprises = new ArrayList<>();

        // 1. Récupérer tous les clients (personnes)
        List<Client> clients = clientRepository.findAll();
        clientsAndEntreprises.addAll(clients);  // Ajouter les clients individuels

        // 2. Récupérer toutes les entreprises (en tant que clients) mais sans leurs clients associés
        List<EntrepriseClient> entreprises = entrepriseClientRepository.findAll();
        clientsAndEntreprises.addAll(entreprises);  // Ajouter les entreprises comme clients sans leurs clients

        return clientsAndEntreprises;
    }
}
