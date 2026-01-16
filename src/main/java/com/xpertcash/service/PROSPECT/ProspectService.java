package com.xpertcash.service.PROSPECT;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xpertcash.DTOs.PROSPECT.ConvertProspectRequestDTO;
import com.xpertcash.DTOs.PROSPECT.CreateInteractionRequestDTO;
import com.xpertcash.DTOs.PROSPECT.CreateProspectRequestDTO;
import com.xpertcash.DTOs.PROSPECT.InteractionDTO;
import com.xpertcash.DTOs.PROSPECT.ProspectAchatDTO;
import com.xpertcash.DTOs.PROSPECT.ProspectDTO;
import com.xpertcash.DTOs.PROSPECT.ProspectPaginatedResponseDTO;
import com.xpertcash.DTOs.PROSPECT.UpdateProspectRequestDTO;
import com.xpertcash.configuration.CentralAccess;
import com.xpertcash.entity.Client;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.entity.Enum.PROSPECT.ProspectType;
import com.xpertcash.entity.PROSPECT.Interaction;
import com.xpertcash.entity.PROSPECT.Prospect;
import com.xpertcash.entity.PROSPECT.ProspectAchat;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.User;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.PROSPECT.InteractionRepository;
import com.xpertcash.repository.PROSPECT.ProspectAchatRepository;
import com.xpertcash.repository.PROSPECT.ProspectRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.service.AuthenticationHelper;

import jakarta.servlet.http.HttpServletRequest;

@Service
@Transactional
public class ProspectService {
    @Autowired
    private ProspectRepository prospectRepository;
    @Autowired
    private InteractionRepository interactionRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private EntrepriseClientRepository entrepriseClientRepository;
    @Autowired
    private ProduitRepository produitRepository;
    @Autowired
    private ProspectAchatRepository prospectAchatRepository;
    @Autowired
    private AuthenticationHelper authHelper;

    /**
     * Créer un nouveau prospect
     */
    public ProspectDTO createProspect(CreateProspectRequestDTO request, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour créer un prospect");
        }

        if (request.getType() == null) {
            throw new IllegalArgumentException("Le type de prospect est obligatoire (PARTICULIER ou ENTREPRISE)");
        }

        if (request.getType() == ProspectType.ENTREPRISE) {
            if (request.getNom() == null || request.getNom().trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom de l'entreprise est obligatoire pour un prospect ENTREPRISE");
            }
        } else if (request.getType() == ProspectType.PARTICULIER) {
            if (request.getNomComplet() == null || request.getNomComplet().trim().isEmpty()) {
                throw new IllegalArgumentException("Le prénom est obligatoire pour un prospect PARTICULIER");
            }
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<Prospect> existingProspect = prospectRepository.findByEmailAndEntrepriseId(request.getEmail(), entreprise.getId());
            if (existingProspect.isPresent()) {
                Prospect prospect = existingProspect.get();
                String prospectName = prospect.getType() == ProspectType.ENTREPRISE 
                    ? prospect.getNom() 
                    : prospect.getNomComplet();
                throw new IllegalArgumentException("Un prospect avec cet email existe déjà dans votre entreprise : " + prospectName + " (ID: " + prospect.getId() + ")");
            }
        }
        
        if (request.getTelephone() != null && !request.getTelephone().trim().isEmpty()) {
            Optional<Prospect> existingProspect = prospectRepository.findByTelephoneAndEntrepriseId(request.getTelephone(), entreprise.getId());
            if (existingProspect.isPresent()) {
                Prospect prospect = existingProspect.get();
                String prospectName = prospect.getType() == ProspectType.ENTREPRISE 
                    ? prospect.getNom() 
                    : prospect.getNomComplet();
                throw new IllegalArgumentException("Un prospect avec ce numéro de téléphone existe déjà dans votre entreprise : " + prospectName + " (ID: " + prospect.getId() + ")");
            }
        }

        Prospect prospect = new Prospect();
        prospect.setType(request.getType());
        prospect.setEntreprise(entreprise);
        
        if (request.getType() == ProspectType.ENTREPRISE) {
            prospect.setNom(request.getNom().trim());
            prospect.setSecteur(request.getSecteur() != null ? request.getSecteur().trim() : null);
            prospect.setAdresse(request.getAdresse() != null ? request.getAdresse().trim() : null);
            prospect.setVille(request.getVille() != null ? request.getVille().trim() : null);
            prospect.setPays(request.getPays() != null ? request.getPays().trim() : null);
        } else if (request.getType() == ProspectType.PARTICULIER) {
            prospect.setNomComplet(request.getNomComplet().trim());
            prospect.setProfession(request.getProfession() != null ? request.getProfession().trim() : null);
            prospect.setVille(request.getVille() != null ? request.getVille().trim() : null);
            prospect.setAdresse(request.getAdresse() != null ? request.getAdresse().trim() : null);
            prospect.setPays(request.getPays() != null ? request.getPays().trim() : null);
        }
        
        prospect.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        prospect.setTelephone(request.getTelephone() != null ? request.getTelephone().trim() : null);
        prospect.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        Prospect savedProspect = prospectRepository.save(prospect);
        return convertToDTO(savedProspect);
    }

 
    public ProspectDTO getProspectById(Long id, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour voir ce prospect");
        }

        Prospect prospect = prospectRepository.findByIdAndEntrepriseId(id, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + id));
        
        return convertToDTO(prospect);
    }

    /**
     * Récupérer tous les prospects avec pagination et interactions
     */
    public ProspectPaginatedResponseDTO getAllProspects(int page, int size, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour voir les prospects");
        }

        if (page < 0) page = 0;
        if (size <= 0) size = 20; 
        if (size > 100) size = 100;

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Prospect> prospectsPage = prospectRepository.findByEntrepriseId(entreprise.getId(), pageable);

        Page<ProspectDTO> prospectDTOPage = prospectsPage.map(this::convertToDTO);
        return ProspectPaginatedResponseDTO.fromPage(prospectDTOPage);
    }



    /**
     * Mettre à jour un prospect
     */
    public ProspectDTO updateProspect(Long id, UpdateProspectRequestDTO request, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour modifier ce prospect");
        }

        Prospect prospect = prospectRepository.findByIdAndEntrepriseId(id, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + id));

        if (request.getType() == null) {
            throw new IllegalArgumentException("Le type de prospect est obligatoire (PARTICULIER ou ENTREPRISE)");
        }

        if (request.getType() == ProspectType.ENTREPRISE) {
            if (request.getNom() == null || request.getNom().trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom de l'entreprise est obligatoire pour un prospect ENTREPRISE");
            }
        } else if (request.getType() == ProspectType.PARTICULIER) {
            if (request.getNomComplet() == null || request.getNomComplet().trim().isEmpty()) {
                throw new IllegalArgumentException("Le prénom est obligatoire pour un prospect PARTICULIER");
            }
            
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<Prospect> existingProspect = prospectRepository.findByEmailAndEntrepriseId(request.getEmail(), entreprise.getId());
            if (existingProspect.isPresent() && !existingProspect.get().getId().equals(id)) {
                throw new IllegalArgumentException("Un autre prospect avec cet email existe déjà dans votre entreprise");
            }
        }
        
        if (request.getTelephone() != null && !request.getTelephone().trim().isEmpty()) {
            Optional<Prospect> existingProspect = prospectRepository.findByTelephoneAndEntrepriseId(request.getTelephone(), entreprise.getId());
            if (existingProspect.isPresent() && !existingProspect.get().getId().equals(id)) {
                throw new IllegalArgumentException("Un autre prospect avec ce numéro de téléphone existe déjà dans votre entreprise");
            }
        }

        prospect.setType(request.getType());
        
        if (request.getType() == ProspectType.ENTREPRISE) {
            prospect.setNom(request.getNom().trim());
            prospect.setSecteur(request.getSecteur() != null ? request.getSecteur().trim() : null);
            prospect.setAdresse(request.getAdresse() != null ? request.getAdresse().trim() : null);
            prospect.setVille(request.getVille() != null ? request.getVille().trim() : null);
            prospect.setPays(request.getPays() != null ? request.getPays().trim() : null);
            // Vider les champs particulier
            prospect.setNomComplet(null);
            prospect.setProfession(null);
        } else if (request.getType() == ProspectType.PARTICULIER) {
            prospect.setNomComplet(request.getNomComplet().trim());
            prospect.setProfession(request.getProfession() != null ? request.getProfession().trim() : null);
            prospect.setAdresse(request.getAdresse() != null ? request.getAdresse().trim() : null);
            prospect.setPays(request.getPays() != null ? request.getPays().trim() : null);
            prospect.setVille(request.getVille() != null ? request.getVille().trim() : null);
            // Vider les champs entreprise
            prospect.setNom(null);
            prospect.setSecteur(null);
        }
        
        prospect.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        prospect.setTelephone(request.getTelephone() != null ? request.getTelephone().trim() : null);
        prospect.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        Prospect updatedProspect = prospectRepository.save(prospect);
        return convertToDTO(updatedProspect);
    }

    /**
     * Supprimer un prospect
     */
    public void deleteProspect(Long id, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entreprise.getId());
        
        if (!isAdmin) {
            throw new RuntimeException("Accès refusé : Seuls les administrateurs peuvent supprimer un prospect");
        }

        Prospect prospect = prospectRepository.findByIdAndEntrepriseId(id, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + id));

        prospectRepository.delete(prospect);
    }

    /**
     * Ajouter une interaction à un prospect
     */
    public InteractionDTO addInteraction(Long prospectId, CreateInteractionRequestDTO request, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour ajouter une interaction");
        }

        Prospect prospect = prospectRepository.findByIdAndEntrepriseId(prospectId, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + prospectId));

        if (request.getType() == null) {
            throw new IllegalArgumentException("Le type d'interaction est obligatoire");
        }
      
        Interaction interaction = new Interaction();
        interaction.setType(request.getType());
        interaction.setNotes(request.getNotes());
        if (request.getAssignedTo() != null && !request.getAssignedTo().trim().isEmpty()) {
            interaction.setAssignedTo(request.getAssignedTo().trim());
        }
        interaction.setNextFollowUp(request.getNextFollowUp());
        interaction.setProspect(prospect);

        if (request.getProduitId() != null) {
            Produit produit = produitRepository.findById(request.getProduitId())
                .orElseThrow(() -> new IllegalArgumentException("Produit/service non trouvé avec l'ID: " + request.getProduitId()));

            if (!produit.getBoutique().getEntreprise().getId().equals(entreprise.getId())) {
                throw new IllegalArgumentException("Ce produit/service n'appartient pas à votre entreprise");
            }

            interaction.setProduit(produit);
        }

        Interaction savedInteraction = interactionRepository.save(interaction);
        return convertInteractionToDTO(savedInteraction);
    }

    /**
     * Récupérer les interactions d'un prospect avec pagination
     */
    public List<InteractionDTO> getProspectInteractions(Long prospectId, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour voir les interactions");
        }

        prospectRepository.findByIdAndEntrepriseId(prospectId, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + prospectId));

        List<Interaction> interactions = interactionRepository.findByProspectIdOrderByOccurredAtDesc(prospectId);

        return interactions.stream()
                .map(this::convertInteractionToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Supprimer une interaction
     */
    public void deleteInteraction(Long interactionId, HttpServletRequest httpRequest) {
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entreprise.getId());
        
        if (!isAdmin) {
            throw new RuntimeException("Accès refusé : Seuls les administrateurs peuvent supprimer une interaction");
        }

        Interaction interaction = interactionRepository.findByIdAndProspectEntrepriseId(interactionId, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Interaction non trouvée avec l'ID: " + interactionId));

        interactionRepository.delete(interaction);
    }

    /**
     * Convertir un prospect en client (après achat)
     */
    public Map<String, Object> convertProspectToClient(Long prospectId, ConvertProspectRequestDTO conversionRequest, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour convertir un prospect en client");
        }

        Prospect prospect = prospectRepository.findByIdAndEntrepriseId(prospectId, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + prospectId));

        if (conversionRequest.getProduitId() == null) {
            throw new IllegalArgumentException("L'ID du produit/service acheté est obligatoire");
        }
        
        Produit produit = produitRepository.findById(conversionRequest.getProduitId())
                .orElseThrow(() -> new IllegalArgumentException("Produit/service non trouvé avec l'ID: " + conversionRequest.getProduitId()));
        
        if (!produit.getBoutique().getEntreprise().getId().equals(entreprise.getId())) {
            throw new IllegalArgumentException("Ce produit/service n'appartient pas à votre entreprise");
        }
        
        Integer quantiteAchetee = conversionRequest.getQuantite() != null ? conversionRequest.getQuantite() : 1;
        
        if (produit.getTypeProduit().name().equals("PHYSIQUE")) {
            if (conversionRequest.getQuantite() == null || conversionRequest.getQuantite() <= 0) {
                throw new IllegalArgumentException("La quantité est obligatoire pour les produits physiques");
            }
            
            if (!produit.getEnStock()) {
                response.put("warning", "Attention: Le produit physique '" + produit.getNom() + "' n'est pas en stock");
            }
            
            if (produit.getQuantite() < quantiteAchetee) {
                response.put("warning", "Attention: Stock insuffisant pour le produit '" + produit.getNom() + 
                    "'. Stock disponible: " + produit.getQuantite() + ", Quantité demandée: " + quantiteAchetee);
            }
        }
        
        Double montantFinal;
        if (conversionRequest.getMontantAchat() != null) {
            montantFinal = conversionRequest.getMontantAchat();
        } else {
            Double prixVente = produit.getPrixVente();
            if (prixVente == null) {
                throw new RuntimeException("Impossible de convertir le prospect car le produit '" + produit.getNom() + "' n'a pas de prix de vente défini.");
            }
            montantFinal = prixVente * quantiteAchetee;
        }

        boolean alreadyConverted = prospect.getConvertedToClient() != null && prospect.getConvertedToClient();

        Long clientId = null;
        String clientType = null;

        if (alreadyConverted) {
            clientId = prospect.getClientId();
            clientType = prospect.getClientType();
            response.put("message", "Prospect déjà converti : ajout d'un nouvel achat");
            response.put("type", prospect.getType() == ProspectType.ENTREPRISE ? "ENTREPRISE" : "PARTICULIER");
        } else {
            if (prospect.getType() == ProspectType.ENTREPRISE) {
                EntrepriseClient entrepriseClient = new EntrepriseClient();
                entrepriseClient.setNom(prospect.getNom());
                entrepriseClient.setEmail(prospect.getEmail());
                entrepriseClient.setTelephone(prospect.getTelephone());
                entrepriseClient.setAdresse(prospect.getAdresse());
                entrepriseClient.setPays(prospect.getPays());
                entrepriseClient.setSecteur(prospect.getSecteur());
                entrepriseClient.setEntreprise(entreprise);
                entrepriseClient.setCreatedAt(LocalDateTime.now());

                EntrepriseClient savedEntrepriseClient = entrepriseClientRepository.save(entrepriseClient);
                clientId = savedEntrepriseClient.getId();
                clientType = "ENTREPRISE_CLIENT";

                response.put("message", "Prospect ENTREPRISE converti en EntrepriseClient avec succès");
                response.put("entrepriseClientId", savedEntrepriseClient.getId());
                response.put("type", "ENTREPRISE");

            } else if (prospect.getType() == ProspectType.PARTICULIER) {
                Client client = new Client();
                client.setNomComplet(prospect.getNomComplet());
                client.setEmail(prospect.getEmail());
                client.setTelephone(prospect.getTelephone());
                client.setAdresse(prospect.getAdresse());
                client.setPays(prospect.getPays());
                client.setEntreprise(entreprise);
                client.setCreatedAt(LocalDateTime.now());

                Client savedClient = clientRepository.save(client);
                clientId = savedClient.getId();
                clientType = "CLIENT";

                response.put("message", "Prospect PARTICULIER converti en Client avec succès");
                response.put("clientId", savedClient.getId());
                response.put("type", "PARTICULIER");
            }
        }
        
        response.put("produitAchete", produit.getNom());
        response.put("produitId", produit.getId());
        response.put("typeProduit", produit.getTypeProduit().name());
        response.put("descriptionProduit", produit.getDescription());
        response.put("prixProduit", produit.getPrixVente() != null ? produit.getPrixVente() : 0.0);
        response.put("quantiteAchetee", quantiteAchetee);
        response.put("montantAchat", montantFinal);
        response.put("notesAchat", conversionRequest.getNotesAchat());
        response.put("dateAchat", LocalDateTime.now().toString());

   
        ProspectAchat achat = new ProspectAchat();
        achat.setProspect(prospect);
        achat.setProduit(produit);
        achat.setQuantite(quantiteAchetee);
        achat.setMontantAchat(montantFinal);
        achat.setNotesAchat(conversionRequest.getNotesAchat());
        achat.setDateAchat(LocalDateTime.now());
        
        achat.setClientId(clientId);
        achat.setClientType(clientType);
        
        prospectAchatRepository.save(achat);

        if (!alreadyConverted) {
            prospect.setConvertedToClient(true);
            prospect.setConvertedAt(LocalDateTime.now());
            prospect.setClientId(clientId);
            prospect.setClientType(clientType);
            prospectRepository.save(prospect);
        }

        return response;
    }



    /**
     * Ajouter un nouvel achat à un prospect déjà converti
     */
    public Map<String, Object> addAchatToConvertedProspect(Long prospectId, ConvertProspectRequestDTO conversionRequest, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour ajouter un achat");
        }

        Prospect prospect = prospectRepository.findByIdAndEntrepriseId(prospectId, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + prospectId));

        if (!prospect.getConvertedToClient()) {
            throw new IllegalArgumentException("Ce prospect n'est pas encore converti en client");
        }

        if (conversionRequest.getProduitId() == null) {
            throw new IllegalArgumentException("L'ID du produit/service acheté est obligatoire");
        }
        
        Produit produit = produitRepository.findById(conversionRequest.getProduitId())
                .orElseThrow(() -> new IllegalArgumentException("Produit/service non trouvé avec l'ID: " + conversionRequest.getProduitId()));
        
        if (!produit.getBoutique().getEntreprise().getId().equals(entreprise.getId())) {
            throw new IllegalArgumentException("Ce produit/service n'appartient pas à votre entreprise");
        }
        
        Integer quantiteAchetee = conversionRequest.getQuantite() != null ? conversionRequest.getQuantite() : 1;
        
        if (produit.getTypeProduit().name().equals("PHYSIQUE")) {
            if (conversionRequest.getQuantite() == null || conversionRequest.getQuantite() <= 0) {
                throw new IllegalArgumentException("La quantité est obligatoire pour les produits physiques");
            }
            
            if (!produit.getEnStock()) {
                response.put("warning", "Attention: Le produit physique '" + produit.getNom() + "' n'est pas en stock");
            }
            
            if (produit.getQuantite() < quantiteAchetee) {
                response.put("warning", "Attention: Stock insuffisant pour le produit '" + produit.getNom() + 
                    "'. Stock disponible: " + produit.getQuantite() + ", Quantité demandée: " + quantiteAchetee);
            }
        }
        
        Double montantFinal;
        if (conversionRequest.getMontantAchat() != null) {
            montantFinal = conversionRequest.getMontantAchat();
        } else {
            Double prixVente = produit.getPrixVente();
            if (prixVente == null) {
                throw new RuntimeException("Impossible de convertir le prospect car le produit '" + produit.getNom() + "' n'a pas de prix de vente défini.");
            }
            montantFinal = prixVente * quantiteAchetee;
        }

        ProspectAchat achat = new ProspectAchat();
        achat.setProspect(prospect);
        achat.setProduit(produit);
        achat.setQuantite(quantiteAchetee);
        achat.setMontantAchat(montantFinal);
        achat.setNotesAchat(conversionRequest.getNotesAchat());
        achat.setDateAchat(LocalDateTime.now());
        achat.setClientId(prospect.getClientId());
        achat.setClientType(prospect.getClientType());
        
        ProspectAchat savedAchat = prospectAchatRepository.save(achat);

        response.put("message", "Nouvel achat ajouté avec succès au prospect converti");
        response.put("achatId", savedAchat.getId());
        response.put("produitAchete", produit.getNom());
        response.put("quantiteAchetee", quantiteAchetee);
        response.put("montantAchat", montantFinal);
        response.put("dateAchat", LocalDateTime.now().toString());

        return response;
    }

    /**
     * Convertir une entité Prospect en DTO
     */
    private ProspectDTO convertToDTO(Prospect prospect) {
        ProspectDTO dto = new ProspectDTO();
        dto.id = prospect.getId();
        dto.type = prospect.getType();
        
        // Champs pour ENTREPRISE
        dto.nom = prospect.getNom();
        dto.secteur = prospect.getSecteur();
        
        // Champs pour PARTICULIER
        dto.nomComplet = prospect.getNomComplet();
        dto.profession = prospect.getProfession();
        
        // Champs communs
        dto.ville = prospect.getVille();
        dto.adresse = prospect.getAdresse();
        dto.pays = prospect.getPays();
      
        
        // Champs communs
        dto.email = prospect.getEmail();
        dto.phone = prospect.getTelephone();
        dto.notes = prospect.getNotes();
        dto.createdAt = prospect.getCreatedAt();
        
        // Statut de conversion
        dto.convertedToClient = prospect.getConvertedToClient();
        dto.convertedAt = prospect.getConvertedAt();
        dto.clientId = prospect.getClientId();
        dto.clientType = prospect.getClientType();

        // Convertir les interactions
        if (prospect.getInteractions() != null) {
            List<InteractionDTO> interactionDTOs = prospect.getInteractions().stream()
                    .map(this::convertInteractionToDTO)
                    .collect(Collectors.toList());
            dto.interactions = interactionDTOs;
        }
        
        // Historique des achats
        if (prospect.getAchats() != null) {
            List<ProspectAchatDTO> achatDTOs = prospect.getAchats().stream()
                    .map(this::convertAchatToDTO)
                    .collect(Collectors.toList());
            dto.achats = achatDTOs;
        }

        return dto;
    }

    /**
     * Convertir une entité Interaction en DTO
     */
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

 
    private ProspectAchatDTO convertAchatToDTO(ProspectAchat achat) {
        ProspectAchatDTO dto = new ProspectAchatDTO();
        dto.id = achat.getId();
        dto.produitId = achat.getProduit().getId();
        dto.produitNom = achat.getProduit().getNom();
        dto.typeProduit = achat.getProduit().getTypeProduit().name();
        dto.descriptionProduit = achat.getProduit().getDescription();
        dto.prixProduit = achat.getProduit().getPrixVente() != null ? achat.getProduit().getPrixVente() : 0.0;
        dto.quantite = achat.getQuantite();
        dto.montantAchat = achat.getMontantAchat();
        dto.notesAchat = achat.getNotesAchat();
        dto.dateAchat = achat.getDateAchat();
        dto.clientId = achat.getClientId();
        dto.clientType = achat.getClientType();
        return dto;
    }
}
