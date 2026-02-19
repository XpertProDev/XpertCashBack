package com.xpertcash.service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.xpertcash.configuration.CentralAccess;

import com.xpertcash.entity.Client;
import com.xpertcash.DTOs.PROSPECT.InteractionDTO;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.User;
import com.xpertcash.entity.PROSPECT.Interaction;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.VENTE.VenteRepository;
import com.xpertcash.repository.PROSPECT.InteractionRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class ClientService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private FactureProformaRepository factureProformaRepository;

    @Autowired
    private FactureReelleRepository factureReelleRepository;
    
    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private VenteRepository venteRepository;


    public Client saveClient(Client client,  HttpServletRequest request) {
            if (client.getNomComplet() == null || client.getNomComplet().trim().isEmpty()) {
            throw new RuntimeException("Le nom du client est obligatoire !");
        }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entrepriseUtilisateur = user.getEntreprise();
    if (entrepriseUtilisateur == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    //  Vérifier que l'utilisateur a le rôle ou la permission appropriée
    // RoleType role = user.getRole().getName();
    // boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    // boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);
    // boolean hasPermissionGestionFacturation = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);


    // if (!isAdminOrManager && !hasPermission && !hasPermissionGestionFacturation) {
    //     throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour créer un client !");
    // }


        client.setEntreprise(entrepriseUtilisateur);

        checkClientExists(client);

        LocalDateTime now = LocalDateTime.now();
        client.setCreatedAt(now);

        if (client.getEntrepriseClient() != null) {
            if (client.getEntrepriseClient().getEntreprise() == null) {
                client.getEntrepriseClient().setEntreprise(entrepriseUtilisateur);
            }
            
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
        Long entrepriseId = client.getEntreprise() != null ? client.getEntreprise().getId() : null;
        if (entrepriseId == null) {
            throw new RuntimeException("Le client doit être associé à une entreprise pour vérifier l'unicité.");
        }

        String email = client.getEmail();
        String telephone = client.getTelephone();

        Optional<Client> existingByEmail = Optional.empty();
        Optional<Client> existingByTelephone = Optional.empty();

        if (email != null && !email.isEmpty()) {
            existingByEmail = clientRepository.findByEmailAndEntrepriseId(email, entrepriseId);
        }

        if (telephone != null && !telephone.isEmpty()) {
            existingByTelephone = clientRepository.findByTelephoneAndEntrepriseId(telephone, entrepriseId);
        }

        if (existingByEmail.isPresent() && existingByTelephone.isPresent()) {
            throw new RuntimeException("Un client avec cet email et ce téléphone existe déjà dans votre entreprise !");
        } else if (existingByEmail.isPresent()) {
            throw new RuntimeException("Un client avec cet email existe déjà dans votre entreprise !");
        } else if (existingByTelephone.isPresent()) {
            throw new RuntimeException("Un client avec ce téléphone existe déjà dans votre entreprise !");
        }
    }

    private void checkEntrepriseExists(EntrepriseClient entrepriseClient) {
        Long entrepriseId = entrepriseClient.getEntreprise() != null ? entrepriseClient.getEntreprise().getId() : null;
        
        if (entrepriseId == null) {
            throw new RuntimeException("L'entreprise cliente doit être associée à une entreprise pour vérifier l'unicité.");
        }

        String email = entrepriseClient.getEmail();
        String telephone = entrepriseClient.getTelephone();

        Optional<EntrepriseClient> existingByEmail = Optional.empty();
        Optional<EntrepriseClient> existingByTelephone = Optional.empty();

        if (email != null && !email.isEmpty()) {
            existingByEmail = entrepriseClientRepository.findByEmailAndEntrepriseId(email, entrepriseId);
        }

        if (telephone != null && !telephone.isEmpty()) {
            existingByTelephone = entrepriseClientRepository.findByTelephoneAndEntrepriseId(telephone, entrepriseId);
        }

        if (existingByEmail.isPresent() && existingByTelephone.isPresent()) {
            throw new RuntimeException("Une entreprise cliente avec cet email et ce téléphone existe déjà dans votre entreprise !");
        } else if (existingByEmail.isPresent()) {
            throw new RuntimeException("Une entreprise cliente avec cet email existe déjà dans votre entreprise !");
        } else if (existingByTelephone.isPresent()) {
            throw new RuntimeException("Une entreprise cliente avec ce téléphone existe déjà dans votre entreprise !");
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
    
    public List<Interaction> getClientInteractions(Long id) {
        return interactionRepository.findByProspectClientIdAndProspectClientTypeOrderByOccurredAtDesc(id, "CLIENT");
    }

    public List<InteractionDTO> getClientInteractionDTOs(Long id) {
        List<Interaction> interactions = getClientInteractions(id);
        return interactions.stream().map(this::convertInteractionToDTO).collect(java.util.stream.Collectors.toList());
    }

    private InteractionDTO convertInteractionToDTO(Interaction interaction) {
        InteractionDTO dto = new InteractionDTO();
        dto.id = interaction.getId();
        dto.type = interaction.getType();
        dto.occurredAt = interaction.getOccurredAt();
        dto.notes = interaction.getNotes();
        dto.assignedTo = interaction.getAssignedTo();
        dto.nextFollowUp = interaction.getNextFollowUp();
        if (interaction.getProduit() != null) {
            dto.produitId = interaction.getProduit().getId();
        }
        return dto;
    }

    /**
     * Retourne les clients de l'entreprise (tenant) d'id entrepriseId.
     * Sécurisé : l'utilisateur ne peut accéder qu'aux clients de sa propre entreprise.
     */
    public List<Client> getClientsByEntreprise(Long entrepriseId, HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        if (user.getEntreprise() == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur.");
        }
        if (!user.getEntreprise().getId().equals(entrepriseId)) {
            throw new RuntimeException("Accès refusé : vous ne pouvez consulter que les clients de votre entreprise.");
        }
        return clientRepository.findClientsByEntrepriseOrEntrepriseClient(entrepriseId);
    }


    public List<Client> getAllClients(HttpServletRequest request) {
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }

    // 2. Vérification des droits
    // boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    // boolean hasPermissionGestionClients = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);
    // boolean hasPermissionGestionFacturation = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);
    


    // // Si l'utilisateur n'est ni Admin, ni Manager, ni n'a la permission de gérer les clients
    // if (!isAdminOrManager && !hasPermissionGestionClients && !hasPermissionGestionFacturation) {
    //     throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires pour consulter les clients.");
    // }

    if (!entreprise.getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès refusé : vous ne pouvez pas accéder aux clients d'une autre entreprise.");
    }

    List<Client> clients = clientRepository.findClientsByEntrepriseOrEntrepriseClient(entreprise.getId());
    
    
    // if (clients.isEmpty()) {
        
    //     List<Client> clientsSansEntreprise = clientRepository.findAll().stream()
    //             .filter(c -> c.getEntreprise() == null && 
    //                        (c.getEntrepriseClient() == null || c.getEntrepriseClient().getEntreprise() == null))
    //             .collect(java.util.stream.Collectors.toList());
        
    //     if (!clientsSansEntreprise.isEmpty()) {
    //         System.out.println(" ATTENTION: " + clientsSansEntreprise.size() + 
    //                          " clients sans entreprise détectés dans la base !");
    //         System.out.println(" Ces clients doivent être associés à une entreprise pour être visibles.");
    //     }
    // }
    
    return clients;
}

    public List<EntrepriseClient> getAllEntrepriseClients(HttpServletRequest request) {
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }

    // Vérification avec CentralAccess
    // boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    // boolean hasPermissionGestionClients = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);

    // if (!isAdminOrManager && !hasPermissionGestionClients) {
    //     throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires pour consulter les clients.");
    // }

    return entrepriseClientRepository.findByEntrepriseId(entreprise.getId());
}


    // Méthode pour récupérer tous les clients (personnes) et entreprises sans leurs clients associés
    public List<Object> getAllClientsAndEntreprises(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        List<Object> clientsAndEntreprises = new ArrayList<>();

        List<Client> clients = clientRepository.findClientsByEntrepriseOrEntrepriseClient(entreprise.getId());
        clientsAndEntreprises.addAll(clients);

        List<EntrepriseClient> entreprises = entrepriseClientRepository.findByEntrepriseId(entreprise.getId());
        clientsAndEntreprises.addAll(entreprises); 

        return clientsAndEntreprises;
    }

    // Méthode pour modifier un client
    @Transactional
    public Client updateClient(Client client, MultipartFile imageClientFile, HttpServletRequest request) {
        if (client.getId() == null) {
            throw new IllegalArgumentException("L'ID du client est obligatoire !");
        }

        Optional<Client> existingClientOpt = clientRepository.findById(client.getId());
        if (existingClientOpt.isEmpty()) {
            throw new EntityNotFoundException("Le client avec cet ID n'existe pas !");
        }

        Client existingClient = existingClientOpt.get();

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        boolean appartientEntreprise = (existingClient.getEntreprise() != null &&
                existingClient.getEntreprise().getId().equals(entreprise.getId())) ||
                (existingClient.getEntrepriseClient() != null &&
                        existingClient.getEntrepriseClient().getEntreprise() != null &&
                        existingClient.getEntrepriseClient().getEntreprise().getId().equals(entreprise.getId()));

        if (!appartientEntreprise) {
            throw new RuntimeException("Accès refusé : ce client ne vous appartient pas.");
        }

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermissionGestionClient = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);

        if (!isAdminOrManager && !hasPermissionGestionClient) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les permissions pour modifier un client.");
        }


        String email = client.getEmail();
        if (email != null && !email.isEmpty()) {
            Optional<Client> clientWithEmail = clientRepository.findByEmailAndEntrepriseId(email, entreprise.getId());
            if (clientWithEmail.isPresent() && !clientWithEmail.get().getId().equals(client.getId())) {
                throw new RuntimeException("Un autre client utilise déjà cet email dans votre entreprise !");
            }
        }

        String telephone = client.getTelephone();
        if (telephone != null && !telephone.isEmpty()) {
            Optional<Client> clientWithTelephone = clientRepository.findByTelephoneAndEntrepriseId(telephone, entreprise.getId());
            if (clientWithTelephone.isPresent() && !clientWithTelephone.get().getId().equals(client.getId())) {
                throw new RuntimeException("Un autre client utilise déjà ce téléphone dans votre entreprise !");
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

        if (client.getEntrepriseClient() == null && existingClient.getEntrepriseClient() != null) {
            existingClient.setEntrepriseClient(null);
        }

        if (imageClientFile != null && !imageClientFile.isEmpty()) {
            String oldImagePath = existingClient.getPhoto(); 
            if (oldImagePath != null && !oldImagePath.isBlank()) {
                Path oldPath = Paths.get("src/main/resources/static" + oldImagePath);
                try {
                    Files.deleteIfExists(oldPath);
                    System.out.println(" Ancienne photo profil supprimée : " + oldImagePath);
                } catch (IOException e) {
                    System.out.println(" Impossible de supprimer l'ancienne photo : " + e.getMessage());
                }
            }

            String newImageUrl = imageStorageService.saveClientImage(imageClientFile);
            existingClient.setPhoto(newImageUrl);
            System.out.println(" Nouvelle photo enregistrée : " + newImageUrl);
        }


        return clientRepository.save(existingClient);
    }


    //Methode pour  supprimer un client qui n'as pas de facture et de commande
    @Transactional
    public void deleteClientIfNoOrdersOrInvoices(Long clientId, HttpServletRequest request) {
        if (clientId == null) {
            throw new IllegalArgumentException("L'ID du client est obligatoire !");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client introuvable avec l'ID : " + clientId));

        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        boolean appartientEntreprise = (client.getEntreprise() != null &&
                client.getEntreprise().getId().equals(entreprise.getId())) ||
                (client.getEntrepriseClient() != null &&
                        client.getEntrepriseClient().getEntreprise() != null &&
                        client.getEntrepriseClient().getEntreprise().getId().equals(entreprise.getId()));

        if (!appartientEntreprise) {
            throw new RuntimeException("Accès refusé : ce client ne vous appartient pas.");
        }

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermissionGestionClient = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);

        if (!isAdminOrManager && !hasPermissionGestionClient) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les permissions pour supprimer un client.");
        }

        boolean hasFactures = factureProformaRepository.existsByClientIdAndEntrepriseId(clientId, entreprise.getId());
        boolean hasFacturesReel = factureReelleRepository.existsByClientIdAndEntrepriseId(clientId, entreprise.getId());
        boolean hasVentes = !venteRepository.findByClientId(clientId).isEmpty();


        if ( hasFactures || hasFacturesReel || hasVentes) {
            throw new RuntimeException("Ce client ne peut pas être supprimé car il est lié à des ventes ou des factures.");
        }

        String imagePath = client.getPhoto();
        if (imagePath != null && !imagePath.isBlank()) {
            Path path = Paths.get("src/main/resources/static" + imagePath);
            try {
                Files.deleteIfExists(path);
                System.out.println(" Photo supprimée : " + imagePath);
            } catch (IOException e) {
                System.out.println(" Erreur lors de la suppression de la photo : " + e.getMessage());
            }
        }

        clientRepository.delete(client);
        System.out.println(" Client supprimé avec succès : " + clientId);
    }

  
            
}