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
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.xpertcash.configuration.CentralAccess;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Client;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.UsersRepository;
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
    private UsersRepository usersRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private FactureProformaRepository factureProformaRepository;

    @Autowired
    private FactureReelleRepository factureReelleRepository;


    public Client saveClient(Client client,  HttpServletRequest request) {
            if (client.getNomComplet() == null || client.getNomComplet().trim().isEmpty()) {
            throw new RuntimeException("Le nom du client est obligatoire !");
        }

    // 🔐 Vérifier la présence du token JWT et récupérer l'ID de l'utilisateur connecté
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long userId;
    try {
        userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'utilisateur depuis le token", e);
    }

    // 🔐 Récupérer l'utilisateur
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable !"));

    // 🔐 Vérifier que l'utilisateur est lié à une entreprise
    Entreprise entrepriseUtilisateur = user.getEntreprise();
    if (entrepriseUtilisateur == null) {
        throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
    }

    // 🔐 Vérifier que l'utilisateur a le rôle ou la permission appropriée
    RoleType role = user.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);
    boolean hasPermissionGestionFacturation = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);


    if (!isAdminOrManager && !hasPermission && !hasPermissionGestionFacturation) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour créer un client !");
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

    // 2. Vérification des droits
    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    boolean hasPermissionGestionClients = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);
    boolean hasPermissionGestionFacturation = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);
    


    // Si l'utilisateur n'est ni Admin, ni Manager, ni n'a la permission de gérer les clients
    if (!isAdminOrManager && !hasPermissionGestionClients && !hasPermissionGestionFacturation) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires pour consulter les clients.");
    }

    // 3. Vérification que l'utilisateur est bien associé à l'entreprise
    if (!entreprise.getId().equals(user.getEntreprise().getId())) {
        throw new RuntimeException("Accès refusé : vous ne pouvez pas accéder aux clients d'une autre entreprise.");
    }

    // 4. Récupérer tous les clients
    List<Client> allClients = clientRepository.findAll();

    // 5. Filtrer les clients associés à l'entreprise de l'utilisateur
    return allClients.stream()
            .filter(c ->
                (c.getEntreprise() != null && c.getEntreprise().getId().equals(entreprise.getId())) ||
                (c.getEntrepriseClient() != null &&
                 c.getEntrepriseClient().getEntreprise() != null &&
                 c.getEntrepriseClient().getEntreprise().getId().equals(entreprise.getId()))
            )
            .collect(Collectors.toList());
}

    //Methode pour recuperer seulement les entreprise client
    public List<EntrepriseClient> getAllEntrepriseClients(HttpServletRequest request) {
    // 1. Extraire le token et l'utilisateur
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

    // Vérification avec CentralAccess
    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    boolean hasPermissionGestionClients = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);

    if (!isAdminOrManager && !hasPermissionGestionClients) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires pour consulter les clients.");
    }

    // 2. Retourner uniquement les EntrepriseClient liés à cette entreprise
    return entrepriseClientRepository.findByEntrepriseId(entreprise.getId());
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


        // 🔐 Authentification de l'utilisateur
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

        // 🔐 Authentification de l'utilisateur
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


        if ( hasFactures || hasFacturesReel) {
            throw new RuntimeException("Ce client ne peut pas être supprimé car il a  des factures.");
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