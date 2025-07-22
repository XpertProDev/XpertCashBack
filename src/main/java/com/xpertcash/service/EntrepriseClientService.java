package com.xpertcash.service;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.configuration.CentralAccess;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Client;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.User;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class EntrepriseClientService {

    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;

    @Autowired
    private UsersRepository usersRepository;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private FactureProformaRepository factureProformaRepository;
    @Autowired
    private FactureReelleRepository factureReelleRepository;

  

   @Transactional
 public EntrepriseClient saveEntreprise(EntrepriseClient entrepriseClient, HttpServletRequest request) {
    // 🔐 Authentification via JWT
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

    // 🔒 Vérifier les droits
    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    boolean hasPermissionGestionClient = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);

    if (!isAdminOrManager && !hasPermissionGestionClient) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les permissions pour créer une entreprise cliente.");
    }

    // ✅ Vérification du nom
    if (entrepriseClient.getNom() == null || entrepriseClient.getNom().trim().isEmpty()) {
        throw new RuntimeException("Le nom de l'entreprise est obligatoire !");
    }

    // ✅ Unicité email / téléphone
    String email = entrepriseClient.getEmail();
    String telephone = entrepriseClient.getTelephone();

    Optional<EntrepriseClient> existingByEmail = Optional.empty();
    Optional<EntrepriseClient> existingByTelephone = Optional.empty();

    if (email != null && !email.isEmpty()) {
        existingByEmail = entrepriseClientRepository.findByEmail(email);
    }

    if (telephone != null && !telephone.isEmpty()) {
        existingByTelephone = entrepriseClientRepository.findByTelephone(telephone);
    }

    if (existingByEmail.isPresent() && existingByTelephone.isPresent()) {
        throw new RuntimeException("Une entreprise avec cet email et ce téléphone existe déjà !");
    } else if (existingByEmail.isPresent()) {
        throw new RuntimeException("Une entreprise avec cet email existe déjà !");
    } else if (existingByTelephone.isPresent()) {
        throw new RuntimeException("Une entreprise avec ce téléphone existe déjà !");
    }

    // 🔗 Lier l’entreprise cliente à l’entreprise de l’utilisateur
    entrepriseClient.setEntreprise(entreprise);
    entrepriseClient.setCreatedAt(LocalDateTime.now());

    // 💾 Enregistrement
    return entrepriseClientRepository.save(entrepriseClient);
}


    public Optional<EntrepriseClient> getEntrepriseById(Long id, HttpServletRequest request) {
    if (id == null) {
        throw new IllegalArgumentException("L'ID de l'entreprise cliente est obligatoire !");
    }

    // 🔐 Authentification JWT
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

    // 🔎 Rechercher l'entreprise cliente
    Optional<EntrepriseClient> entrepriseClientOpt = entrepriseClientRepository.findById(id);
    if (entrepriseClientOpt.isEmpty()) {
        throw new EntityNotFoundException("Entreprise cliente introuvable avec l'ID : " + id);
    }

    EntrepriseClient entrepriseClient = entrepriseClientOpt.get();

    // 🔐 Vérifier que l’entreprise cliente appartient à la même entreprise
    if (entrepriseClient.getEntreprise() == null ||
        !entrepriseClient.getEntreprise().getId().equals(entreprise.getId())) {
        throw new RuntimeException("Accès refusé : cette entreprise cliente ne vous appartient pas.");
    }

    return Optional.of(entrepriseClient);
}


   public List<EntrepriseClient> getAllEntreprises(HttpServletRequest request) {
    // 🔐 Authentification JWT
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

    // 🔒 Autorisation
    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    boolean hasPermissionGestionClient = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);

    if (!isAdminOrManager && !hasPermissionGestionClient) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour voir les entreprises clientes.");
    }

    // 🔎 Récupération filtrée
    return entrepriseClientRepository.findByEntrepriseId(entreprise.getId());
}


     //Methode pour modifier une Entreprise client
    public EntrepriseClient updateEntrepriseClient(EntrepriseClient entrepriseClient) {
        if (entrepriseClient.getId() == null) {
            throw new IllegalArgumentException("L'ID d'entreprise est obligatoire !");
        }
    
        //  si l'entreprise client existe
        Optional<EntrepriseClient> existingEntrepriseClient = entrepriseClientRepository.findById(entrepriseClient.getId());
        if (existingEntrepriseClient.isEmpty()) {
            throw new EntityNotFoundException("L'entreprise avec cet ID n'existe pas !");
        }
    
        EntrepriseClient updateEntrepriseClient = existingEntrepriseClient.get();
    
        // Utilisation de la réflexion pour mettre à jour seulement les champs non null
        for (Field field : EntrepriseClient.class.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object newValue = field.get(entrepriseClient);
                if (newValue != null) {
                    field.set(updateEntrepriseClient, newValue);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    
        // Enregistrer les modifications
        return entrepriseClientRepository.save(updateEntrepriseClient);
    }


 //Methode pour  supprimer un client qui n'as pas de facture et de commande

 @Transactional
public void deleteEntrepriseClientIfNoOrdersOrInvoices(Long entrepriseClientId, HttpServletRequest request) {
    if (entrepriseClientId == null) {
        throw new IllegalArgumentException("L'ID du client entreprise est obligatoire !");
    }

    EntrepriseClient entrepriseClient = entrepriseClientRepository.findById(entrepriseClientId)
            .orElseThrow(() -> new EntityNotFoundException("Client entreprise introuvable avec l'ID : " + entrepriseClientId));

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

    // 🔒 Vérifier que le client entreprise appartient bien à cette entreprise
    if (entrepriseClient.getEntreprise() == null ||
        !entrepriseClient.getEntreprise().getId().equals(entreprise.getId())) {
        throw new RuntimeException("Accès refusé : ce client entreprise ne vous appartient pas.");
    }

    // 🔒 Vérifier que l'utilisateur a les droits
    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    boolean hasPermissionGestionClient = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);

    if (!isAdminOrManager && !hasPermissionGestionClient) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les permissions pour supprimer un client entreprise.");
    }

    // ❌ Vérifier qu’il n’a pas de commandes ni de factures
    boolean hasFactures = factureProformaRepository.existsByEntrepriseClientId(entrepriseClientId);
    boolean hasFacturesReel = factureReelleRepository.existsByEntrepriseClientId(entrepriseClientId);

    if (hasFactures || hasFacturesReel) {
        throw new RuntimeException("Ce client entreprise ne peut pas être supprimé car il a des factures.");
    }

    // ✅ Suppression
    entrepriseClientRepository.delete(entrepriseClient);
    System.out.println("✅ Client entreprise supprimé avec succès : " + entrepriseClientId);
}

  
}
