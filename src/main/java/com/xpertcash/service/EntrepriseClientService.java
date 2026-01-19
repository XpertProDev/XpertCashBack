package com.xpertcash.service;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.xpertcash.configuration.CentralAccess;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.User;
import com.xpertcash.entity.PROSPECT.Interaction;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.FactureReelleRepository;
import com.xpertcash.repository.PROSPECT.InteractionRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

@Service
public class EntrepriseClientService {

    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;

    @Autowired
    private AuthenticationHelper authHelper;

    @Autowired
    private FactureProformaRepository factureProformaRepository;
    @Autowired
    private FactureReelleRepository factureReelleRepository;
    @Autowired
    private InteractionRepository interactionRepository;

  
   @Transactional
    public EntrepriseClient saveEntreprise(EntrepriseClient entrepriseClient, HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
        }

        //  Vérifier les droits
        // boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        // boolean hasPermissionGestionClient = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);
        // boolean hasPermissionGestionFacturation = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);


        // if (!isAdminOrManager && !hasPermissionGestionClient && !hasPermissionGestionFacturation) {
        //     throw new RuntimeException("Accès refusé : vous n'avez pas les permissions pour créer une entreprise cliente.");
        // }

        if (entrepriseClient.getNom() == null || entrepriseClient.getNom().trim().isEmpty()) {
            throw new RuntimeException("Le nom de l'entreprise est obligatoire !");
        }

        //  Unicité email / téléphone
        String email = entrepriseClient.getEmail();
        String telephone = entrepriseClient.getTelephone();

        Optional<EntrepriseClient> existingByEmail = Optional.empty();
        Optional<EntrepriseClient> existingByTelephone = Optional.empty();

        Long entrepriseId = user.getEntreprise().getId();

        if (email != null && !email.isEmpty()) {
            existingByEmail = entrepriseClientRepository.findByEmailAndEntrepriseId(email, entrepriseId);
        }

        if (telephone != null && !telephone.isEmpty()) {
            existingByTelephone = entrepriseClientRepository.findByTelephoneAndEntrepriseId(telephone, entrepriseId);
        }

        if (existingByEmail.isPresent() && existingByTelephone.isPresent()) {
            throw new RuntimeException("Une entreprise avec cet email et ce téléphone existe déjà !");
        } else if (existingByEmail.isPresent()) {
            throw new RuntimeException("Une entreprise avec cet email existe déjà !");
        } else if (existingByTelephone.isPresent()) {
            throw new RuntimeException("Une entreprise avec ce téléphone existe déjà !");
        }

        entrepriseClient.setEntreprise(entreprise);
        entrepriseClient.setCreatedAt(LocalDateTime.now());

        return entrepriseClientRepository.save(entrepriseClient);
    }


    public Optional<EntrepriseClient> getEntrepriseById(Long id, HttpServletRequest request) {
    if (id == null) {
        throw new IllegalArgumentException("L'ID de l'entreprise cliente est obligatoire !");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }

    Optional<EntrepriseClient> entrepriseClientOpt = entrepriseClientRepository.findById(id);
    if (entrepriseClientOpt.isEmpty()) {
        throw new EntityNotFoundException("Entreprise cliente introuvable avec l'ID : " + id);
    }

    EntrepriseClient entrepriseClient = entrepriseClientOpt.get();

    if (entrepriseClient.getEntreprise() == null ||
        !entrepriseClient.getEntreprise().getId().equals(entreprise.getId())) {
        throw new RuntimeException("Accès refusé : cette entreprise cliente ne vous appartient pas.");
    }

    return Optional.of(entrepriseClient);
}

   //Methode pour recuperer les interactions d'une entreprise cliente
   public List<Interaction> getEntrepriseClientInteractions(Long id) {
       return interactionRepository.findByProspectClientIdAndProspectClientTypeOrderByOccurredAtDesc(id, "ENTREPRISE_CLIENT");
   }

   public List<EntrepriseClient> getAllEntreprises(HttpServletRequest request) {
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }
 
    //  Autorisation
    // boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    // boolean hasPermissionGestionClient = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);
    // boolean hasPermissionGestionFacturation = user.getRole().hasPermission(PermissionType.GESTION_FACTURATION);


    // if (!isAdminOrManager && !hasPermissionGestionClient  && !hasPermissionGestionFacturation) {
    //     throw new RuntimeException("Accès refusé : vous n'avez pas les droits pour voir les entreprises clientes.");
    // }

    //  Récupération filtrée
    return entrepriseClientRepository.findByEntrepriseId(entreprise.getId());
}


     //Methode pour modifier une Entreprise client
    public EntrepriseClient updateEntrepriseClient(EntrepriseClient entrepriseClient) {
        if (entrepriseClient.getId() == null) {
            throw new IllegalArgumentException("L'ID d'entreprise est obligatoire !");
        }
    
        Optional<EntrepriseClient> existingEntrepriseClient = entrepriseClientRepository.findById(entrepriseClient.getId());
        if (existingEntrepriseClient.isEmpty()) {
            throw new EntityNotFoundException("L'entreprise avec cet ID n'existe pas !");
        }
    
        EntrepriseClient updateEntrepriseClient = existingEntrepriseClient.get();
    
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

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur");
    }

    if (entrepriseClient.getEntreprise() == null ||
        !entrepriseClient.getEntreprise().getId().equals(entreprise.getId())) {
        throw new RuntimeException("Accès refusé : ce client entreprise ne vous appartient pas.");
    }

    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    boolean hasPermissionGestionClient = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);

    if (!isAdminOrManager && !hasPermissionGestionClient) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les permissions pour supprimer un client entreprise.");
    }

    boolean hasFactures = factureProformaRepository.existsByEntrepriseClientIdAndEntrepriseId(entrepriseClientId, entreprise.getId());
    boolean hasFacturesReel = factureReelleRepository.existsByEntrepriseClientIdAndEntrepriseId(entrepriseClientId, entreprise.getId());

    if (hasFactures || hasFacturesReel) {
        throw new RuntimeException("Ce client entreprise ne peut pas être supprimé car il a des factures.");
    }

    entrepriseClientRepository.delete(entrepriseClient);
    System.out.println(" Client entreprise supprimé avec succès : " + entrepriseClientId);
}

  
}
