package com.xpertcash.service;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Client;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.entity.User;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private JwtUtil jwtUtil;

  
    public Client saveClient(Client client,  HttpServletRequest request) {
        if (client.getNomComplet() == null || client.getNomComplet().trim().isEmpty()) {
            throw new RuntimeException("Le nom du client est obligatoire !");
        }

               // Vérifier la présence du token JWT et récupérer l'ID de l'utilisateur connecté
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId = null;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'utilisateur depuis le token", e);
    }

    // Récupérer l'utilisateur par son ID
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

    // Vérifier que l'utilisateur a une entreprise associée (entreprise créatrice de la facture)
    Entreprise entrepriseUtilisateur = user.getEntreprise();
    if (entrepriseUtilisateur == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    client.setEntreprise(entrepriseUtilisateur);
    
        checkClientExists(client);

        LocalDateTime now = LocalDateTime.now();
        client.setCreatedAt(now);
    
        if (client.getEntrepriseClient() != null) {
            checkEntrepriseExists(client.getEntrepriseClient());
    
            if (client.getEntrepriseClient().getId() != null) {
                associateExistingEntreprise(client);
            } else {
                saveNewEntreprise(client);
            }
        }
    
        return clientRepository.save(client);
    }
    
    private void checkClientExists(Client client) {
        String email = client.getEmail();
        String telephone = client.getTelephone();
    
        Optional<Client> existingByEmail = Optional.empty();
        Optional<Client> existingByTelephone = Optional.empty();
    
        if (email != null && !email.isEmpty()) {
            existingByEmail = clientRepository.findByEmail(email);
        }
    
        if (telephone != null && !telephone.isEmpty()) {
            existingByTelephone = clientRepository.findByTelephone(telephone);
        }
    
        if (existingByEmail.isPresent() && existingByTelephone.isPresent()) {
            throw new RuntimeException("Un client avec cet email et ce téléphone existe déjà !");
        } else if (existingByEmail.isPresent()) {
            throw new RuntimeException("Un client avec cet email existe déjà !");
        } else if (existingByTelephone.isPresent()) {
            throw new RuntimeException("Un client avec ce téléphone existe déjà !");
        }
    } 
    
    private void checkEntrepriseExists(EntrepriseClient entrepriseClient) {
        String email = entrepriseClient.getEmail();
        String telephone = entrepriseClient.getTelephone();
    
        Optional<EntrepriseClient> existingByEmail = Optional.empty();
        Optional<EntrepriseClient> existingByTelephone = Optional.empty();
    
        // Vérifier si l'email est renseigné et existe déjà
        if (email != null && !email.isEmpty()) {
            existingByEmail = entrepriseClientRepository.findByEmail(email);
        }
    
        // Vérifier si le téléphone est renseigné et existe déjà
        if (telephone != null && !telephone.isEmpty()) {
            existingByTelephone = entrepriseClientRepository.findByTelephone(telephone);
        }
    
        // Construire un message d'erreur précis
        if (existingByEmail.isPresent() && existingByTelephone.isPresent()) {
            throw new RuntimeException("Une entreprise avec cet email et ce téléphone existe déjà !");
        } else if (existingByEmail.isPresent()) {
            throw new RuntimeException("Une entreprise avec cet email existe déjà !");
        } else if (existingByTelephone.isPresent()) {
            throw new RuntimeException("Une entreprise avec ce téléphone existe déjà !");
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
        if (client.getEntrepriseClient() != null) {
            client.getEntrepriseClient().setCreatedAt(client.getCreatedAt());
            EntrepriseClient savedEntreprise = entrepriseClientRepository.save(client.getEntrepriseClient());
            client.setEntrepriseClient(savedEntreprise);
        }
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


    public List<Client> getAllClients(HttpServletRequest request) {
    // 1. Extraire l'utilisateur à partir du token
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur", e);
    }

    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }

    // 2. Récupérer tous les clients
    List<Client> allClients = clientRepository.findAll();

    // 3. Filtrer : client lié à entreprise OU entrepriseClient liée à l'entreprise
    return allClients.stream()
            .filter(c ->
                (c.getEntreprise() != null && c.getEntreprise().getId().equals(entreprise.getId())) ||
                (c.getEntrepriseClient() != null && 
                 c.getEntrepriseClient().getEntreprise() != null &&
                 c.getEntrepriseClient().getEntreprise().getId().equals(entreprise.getId()))
            )
            .collect(Collectors.toList());


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


        // Méthode pour modifier un client 
        public Client updateClient(Client client) {
    if (client.getId() == null) {
        throw new IllegalArgumentException("L'ID du client est obligatoire !");
    }

    Optional<Client> existingClientOpt = clientRepository.findById(client.getId());
    if (existingClientOpt.isEmpty()) {
        throw new EntityNotFoundException("Le client avec cet ID n'existe pas !");
    }

    Client existingClient = existingClientOpt.get();

    // Vérifier unicité de l'email (hors lui-même)
    String email = client.getEmail();
    if (email != null && !email.isEmpty()) {
        Optional<Client> clientWithEmail = clientRepository.findByEmail(email);
        if (clientWithEmail.isPresent() && !clientWithEmail.get().getId().equals(client.getId())) {
            throw new RuntimeException("Un autre client utilise déjà cet email !");
        }
    }

    // Vérifier unicité du téléphone (hors lui-même)
    String telephone = client.getTelephone();
    if (telephone != null && !telephone.isEmpty()) {
        Optional<Client> clientWithTelephone = clientRepository.findByTelephone(telephone);
        if (clientWithTelephone.isPresent() && !clientWithTelephone.get().getId().equals(client.getId())) {
            throw new RuntimeException("Un autre client utilise déjà ce téléphone !");
        }
    }

    // Mise à jour des champs non nuls
    for (Field field : Client.class.getDeclaredFields()) {
        field.setAccessible(true);
        try {
            Object newValue = field.get(client);
            if (newValue != null) {
                field.set(existingClient, newValue);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    //  Nouveau bloc : détacher l'entreprise si elle est explicitement mise à null
    if (client.getEntrepriseClient() == null && existingClient.getEntrepriseClient() != null) {
        existingClient.setEntrepriseClient(null);
    }

    return clientRepository.save(existingClient);
}

}
