package com.xpertcash.service;

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
        // Vérifie si un objet EntrepriseClient est associé au client
        if (client.getEntrepriseClient() != null) {
            // Si l'entreprise a un ID (elle existe déjà dans la base de données)
            if (client.getEntrepriseClient().getId() != null) {
                // Vérifier si l'entreprise existe déjà dans la base de données avec son ID
                Optional<EntrepriseClient> existingEntreprise = entrepriseClientRepository
                        .findById(client.getEntrepriseClient().getId());

                if (existingEntreprise.isPresent()) {
                    // Associer l'entreprise existante au client sans modifier ses informations
                    client.setEntrepriseClient(existingEntreprise.get());
                } else {
                    // Si l'entreprise n'existe pas dans la base de données (ID est valide mais entreprise non trouvée)
                    throw new IllegalArgumentException("L'entreprise avec cet ID n'existe pas.");
                }
            } else {
                // Si l'entreprise n'a pas d'ID (c'est une nouvelle entreprise)
                // Sauvegarde l'entreprise avant d'associer au client
                EntrepriseClient savedEntreprise = entrepriseClientRepository.save(client.getEntrepriseClient());
                client.setEntrepriseClient(savedEntreprise);  // Associer l'entreprise nouvellement créée
            }
        } else {
            // Si aucune entreprise n'est fournie, le client est simplement enregistré sans entreprise
        }

        // Sauvegarde du client dans la base de données
        return clientRepository.save(client);
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
