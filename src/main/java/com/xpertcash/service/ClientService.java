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

    // 🔐 Vérifier que l'utilisateur est lié à une entreprise
    Entreprise entrepriseUtilisateur = user.getEntreprise();
    if (entrepriseUtilisateur == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    // 🔐 Vérifier que l'utilisateur a le rôle ou la permission appropriée
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
            // Assigner l'entreprise à l'EntrepriseClient avant de vérifier l'unicité
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
        // Vérifier l'unicité uniquement dans l'entreprise du client
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
        // Vérifier l'unicité uniquement dans l'entreprise de l'utilisateur connecté
        Long entrepriseId = entrepriseClient.getEntreprise() != null ? entrepriseClient.getEntreprise().getId() : null;
        
        if (entrepriseId == null) {
            throw new RuntimeException("L'entreprise cliente doit être associée à une entreprise pour vérifier l'unicité.");
        }

        String email = entrepriseClient.getEmail();
        String telephone = entrepriseClient.getTelephone();

        Optional<EntrepriseClient> existingByEmail = Optional.empty();
        Optional<EntrepriseClient> existingByTelephone = Optional.empty();

        // Vérifier si l'email est renseigné et existe déjà dans cette entreprise
        if (email != null && !email.isEmpty()) {
            existingByEmail = entrepriseClientRepository.findByEmailAndEntrepriseId(email, entrepriseId);
        }

        // Vérifier si le téléphone est renseigné et existe déjà dans cette entreprise
        if (telephone != null && !telephone.isEmpty()) {
            existingByTelephone = entrepriseClientRepository.findByTelephoneAndEntrepriseId(telephone, entrepriseId);
        }

        // Construire un message d'erreur précis
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
    
    //Methode pour recuperer les interactions d'un client (entities)
    public List<Interaction> getClientInteractions(Long id) {
        return interactionRepository.findByProspectClientIdAndProspectClientTypeOrderByOccurredAtDesc(id, "CLIENT");
    }

    //Methode pour recuperer les interactions d'un client en DTO (inclut produitId)
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

    public List<Client> getClientsByEntreprise(Long entrepriseId) {
        return clientRepository.findByEntrepriseClientId(entrepriseId);
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

    // 3. Vérification que l'utilisateur est bien associé à l'entreprise
    if (!entreprise.getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès refusé : vous ne pouvez pas accéder aux clients d'une autre entreprise.");
    }

    // 4. Récupérer uniquement les clients de l'entreprise (optimisé avec requête SQL)
    List<Client> clients = clientRepository.findClientsByEntrepriseOrEntrepriseClient(entreprise.getId());
    
    // Log pour déboguer
    System.out.println("🔍 Entreprise ID: " + entreprise.getId());
    System.out.println("📊 Nombre de clients trouvés: " + clients.size());
    
    // Si aucun client trouvé, vérifier s'il y a des clients sans entreprise
    if (clients.isEmpty()) {
        long totalClients = clientRepository.count();
        long clientsDirect = clientRepository.countClientsDirectByEntrepriseId(entreprise.getId());
        long clientsViaEntreprise = clientRepository.countClientsEntrepriseByEntrepriseId(entreprise.getId());
        
        System.out.println("⚠️ Aucun client trouvé pour l'entreprise " + entreprise.getId());
        System.out.println("📈 Total clients dans la base: " + totalClients);
        System.out.println("📈 Clients directs de l'entreprise: " + clientsDirect);
        System.out.println("📈 Clients via EntrepriseClient: " + clientsViaEntreprise);
        
        // Vérifier s'il y a des clients sans entreprise (anciens clients avant l'isolation)
        List<Client> clientsSansEntreprise = clientRepository.findAll().stream()
                .filter(c -> c.getEntreprise() == null && 
                           (c.getEntrepriseClient() == null || c.getEntrepriseClient().getEntreprise() == null))
                .collect(java.util.stream.Collectors.toList());
        
        if (!clientsSansEntreprise.isEmpty()) {
            System.out.println("⚠️ ATTENTION: " + clientsSansEntreprise.size() + 
                             " clients sans entreprise détectés dans la base !");
            System.out.println("💡 Ces clients doivent être associés à une entreprise pour être visibles.");
        }
    }
    
    return clients;
}

    //Methode pour recuperer seulement les entreprise client
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

    // 2. Retourner uniquement les EntrepriseClient liés à cette entreprise
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

        // 1. Récupérer uniquement les clients (personnes) de cette entreprise
        List<Client> clients = clientRepository.findClientsByEntrepriseOrEntrepriseClient(entreprise.getId());
        clientsAndEntreprises.addAll(clients);  // Ajouter les clients individuels

        // 2. Récupérer uniquement les entreprises clientes de cette entreprise
        List<EntrepriseClient> entreprises = entrepriseClientRepository.findByEntrepriseId(entreprise.getId());
        clientsAndEntreprises.addAll(entreprises);  // Ajouter les entreprises comme clients sans leurs clients

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

        // 🔒 Vérifier que le client appartient à cette entreprise
        boolean appartientEntreprise = (existingClient.getEntreprise() != null &&
                existingClient.getEntreprise().getId().equals(entreprise.getId())) ||
                (existingClient.getEntrepriseClient() != null &&
                        existingClient.getEntrepriseClient().getEntreprise() != null &&
                        existingClient.getEntrepriseClient().getEntreprise().getId().equals(entreprise.getId()));

        if (!appartientEntreprise) {
            throw new RuntimeException("Accès refusé : ce client ne vous appartient pas.");
        }

        // 🔒 Vérifier que l'utilisateur a les droits
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermissionGestionClient = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);

        if (!isAdminOrManager && !hasPermissionGestionClient) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les permissions pour modifier un client.");
        }


        // Vérifier unicité de l'email (hors lui-même) uniquement dans cette entreprise
        String email = client.getEmail();
        if (email != null && !email.isEmpty()) {
            Optional<Client> clientWithEmail = clientRepository.findByEmailAndEntrepriseId(email, entreprise.getId());
            if (clientWithEmail.isPresent() && !clientWithEmail.get().getId().equals(client.getId())) {
                throw new RuntimeException("Un autre client utilise déjà cet email dans votre entreprise !");
            }
        }

        // Vérifier unicité du téléphone (hors lui-même) uniquement dans cette entreprise
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

        //  Nouveau bloc : détacher l'entreprise si elle est explicitement mise à null
        if (client.getEntrepriseClient() == null && existingClient.getEntrepriseClient() != null) {
            existingClient.setEntrepriseClient(null);
        }

        // Mise à jour de la photo si image présente
        if (imageClientFile != null && !imageClientFile.isEmpty()) {
            String oldImagePath = existingClient.getPhoto(); // ✅ Prendre depuis l'objet actuel en base
            if (oldImagePath != null && !oldImagePath.isBlank()) {
                Path oldPath = Paths.get("src/main/resources/static" + oldImagePath);
                try {
                    Files.deleteIfExists(oldPath);
                    System.out.println("🗑️ Ancienne photo profil supprimée : " + oldImagePath);
                } catch (IOException e) {
                    System.out.println("⚠️ Impossible de supprimer l'ancienne photo : " + e.getMessage());
                }
            }

            String newImageUrl = imageStorageService.saveClientImage(imageClientFile);
            existingClient.setPhoto(newImageUrl);
            System.out.println("📸 Nouvelle photo enregistrée : " + newImageUrl);
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

        // 🔒 Vérification que le client appartient bien à cette entreprise
        boolean appartientEntreprise = (client.getEntreprise() != null &&
                client.getEntreprise().getId().equals(entreprise.getId())) ||
                (client.getEntrepriseClient() != null &&
                        client.getEntrepriseClient().getEntreprise() != null &&
                        client.getEntrepriseClient().getEntreprise().getId().equals(entreprise.getId()));

        if (!appartientEntreprise) {
            throw new RuntimeException("Accès refusé : ce client ne vous appartient pas.");
        }

        // 🔒 Vérifier que l'utilisateur a les droits
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermissionGestionClient = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);

        if (!isAdminOrManager && !hasPermissionGestionClient) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les permissions pour supprimer un client.");
        }

        // ❌ Vérifier que le client n’a pas de commandes ou de factures
        boolean hasFactures = factureProformaRepository.existsByClientId(clientId);
        boolean hasFacturesReel = factureReelleRepository.existsByClientId(clientId);
        boolean hasVentes = !venteRepository.findByClientId(clientId).isEmpty();


        if ( hasFactures || hasFacturesReel || hasVentes) {
            throw new RuntimeException("Ce client ne peut pas être supprimé car il est lié à des ventes ou des factures.");
        }

        // 🗑️ Supprimer l’image si elle existe
        String imagePath = client.getPhoto();
        if (imagePath != null && !imagePath.isBlank()) {
            Path path = Paths.get("src/main/resources/static" + imagePath);
            try {
                Files.deleteIfExists(path);
                System.out.println("🗑️ Photo supprimée : " + imagePath);
            } catch (IOException e) {
                System.out.println("⚠️ Erreur lors de la suppression de la photo : " + e.getMessage());
            }
        }

        clientRepository.delete(client);
        System.out.println("✅ Client supprimé avec succès : " + clientId);
    }

  
  // Pour cas special de permission
            
}