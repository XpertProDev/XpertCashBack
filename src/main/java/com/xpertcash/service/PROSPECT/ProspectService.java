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
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.User;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.repository.ClientRepository;
import com.xpertcash.repository.EntrepriseClientRepository;
import com.xpertcash.repository.PROSPECT.InteractionRepository;
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
    private AuthenticationHelper authHelper;

    /**
     * Créer un nouveau prospect
     */
    public ProspectDTO createProspect(CreateProspectRequestDTO request, HttpServletRequest httpRequest) {
        // --- 1. Extraction et validation du token JWT ---
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        // --- 2. Vérification des permissions ---
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour créer un prospect");
        }

        // --- 3. Validation des données ---
        if (request.getType() == null) {
            throw new IllegalArgumentException("Le type de prospect est obligatoire (PARTICULIER ou ENTREPRISE)");
        }

        // Validation selon le type
        if (request.getType() == ProspectType.ENTREPRISE) {
            if (request.getNom() == null || request.getNom().trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom de l'entreprise est obligatoire pour un prospect ENTREPRISE");
            }
        } else if (request.getType() == ProspectType.PARTICULIER) {
            if (request.getNomComplet() == null || request.getNomComplet().trim().isEmpty()) {
                throw new IllegalArgumentException("Le prénom est obligatoire pour un prospect PARTICULIER");
            }
        }

        // --- 4. Vérifier si un prospect avec le même email existe déjà dans l'entreprise ---
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<Prospect> existingProspect = prospectRepository.findByEmailAndEntrepriseId(request.getEmail(), entreprise.getId());
            if (existingProspect.isPresent()) {
                throw new IllegalArgumentException("Un prospect avec cet email existe déjà dans votre entreprise");
            }
        }

        // --- 5. Créer le prospect ---
        Prospect prospect = new Prospect();
        prospect.setType(request.getType());
        prospect.setEntreprise(entreprise);
        
        // Remplir les champs selon le type
        if (request.getType() == ProspectType.ENTREPRISE) {
            prospect.setNom(request.getNom().trim());
            prospect.setSector(request.getSecter() != null ? request.getSecter().trim() : null);
            prospect.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
            prospect.setCity(request.getCity() != null ? request.getCity().trim() : null);
            prospect.setCountry(request.getCountry() != null ? request.getCountry().trim() : null);
        } else if (request.getType() == ProspectType.PARTICULIER) {
            prospect.setNomComplet(request.getNomComplet().trim());
            prospect.setAdresse(request.getAdresse() != null ? request.getAdresse().trim() : null);
            prospect.setPays(request.getPays() != null ? request.getPays().trim() : null);
        }
        
        // Champs communs
        prospect.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        prospect.setTelephone(request.getTelephone() != null ? request.getTelephone().trim() : null);
        prospect.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        Prospect savedProspect = prospectRepository.save(prospect);
        return convertToDTO(savedProspect);
    }

    /**
     * Récupérer un prospect par son ID avec ses interactions
     */
    public ProspectDTO getProspectById(Long id, HttpServletRequest httpRequest) {
        // --- 1. Extraction et validation du token JWT ---
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        // --- 2. Vérification des permissions ---
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour voir ce prospect");
        }

        // --- 3. Récupérer le prospect avec vérification d'appartenance ---
        Prospect prospect = prospectRepository.findByIdAndEntrepriseId(id, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + id));
        
        return convertToDTO(prospect);
    }

    /**
     * Récupérer tous les prospects avec pagination et interactions
     */
    public ProspectPaginatedResponseDTO getAllProspects(int page, int size, HttpServletRequest httpRequest) {
        // --- 1. Extraction et validation du token JWT ---
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        // --- 2. Vérification des permissions ---
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour voir les prospects");
        }

        // --- 3. Validation des paramètres de pagination ---
        if (page < 0) page = 0;
        if (size <= 0) size = 20; // Taille par défaut
        if (size > 100) size = 100; // Limite maximale

        // --- 4. Créer le Pageable avec tri par date de création décroissante ---
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // --- 5. Récupérer les prospects avec pagination et filtrage par entreprise ---
        Page<Prospect> prospectsPage = prospectRepository.findByEntrepriseId(entreprise.getId(), pageable);

        // --- 6. Créer la réponse paginée ---
        Page<ProspectDTO> prospectDTOPage = prospectsPage.map(this::convertToDTO);
        return ProspectPaginatedResponseDTO.fromPage(prospectDTOPage);
    }

    /**
     * Récupérer les prospects par type avec pagination
     */
    public ProspectPaginatedResponseDTO getProspectsByType(ProspectType type, int page, int size, HttpServletRequest httpRequest) {
        // --- 1. Extraction et validation du token JWT ---
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        // --- 2. Vérification des permissions ---
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour voir les prospects");
        }

        // --- 3. Validation des paramètres de pagination ---
        if (page < 0) page = 0;
        if (size <= 0) size = 20; // Taille par défaut
        if (size > 100) size = 100; // Limite maximale

        // --- 4. Créer le Pageable avec tri par date de création décroissante ---
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // --- 5. Récupérer les prospects avec filtrage par type et entreprise ---
        Page<Prospect> prospectsPage = prospectRepository.findByEntrepriseIdAndType(entreprise.getId(), type, pageable);

        // --- 6. Créer la réponse paginée ---
        Page<ProspectDTO> prospectDTOPage = prospectsPage.map(this::convertToDTO);
        return ProspectPaginatedResponseDTO.fromPage(prospectDTOPage);
    }

    /**
     * Rechercher des prospects par nom/prénom avec pagination
     */
    public ProspectPaginatedResponseDTO searchProspects(String query, int page, int size, HttpServletRequest httpRequest) {
        // --- 1. Extraction et validation du token JWT ---
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        // --- 2. Vérification des permissions ---
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour rechercher des prospects");
        }

        // --- 3. Validation des paramètres de pagination ---
        if (page < 0) page = 0;
        if (size <= 0) size = 20; // Taille par défaut
        if (size > 100) size = 100; // Limite maximale

        // --- 4. Créer le Pageable avec tri par nom d'entreprise ---
        Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());

        // --- 5. Rechercher les prospects avec filtrage par entreprise ---
        // Recherche dans les noms d'entreprise ET les noms/prénoms
        Page<Prospect> prospectsPage = prospectRepository.findByEntrepriseIdAndNomContainingIgnoreCase(entreprise.getId(), query, pageable);

        // --- 6. Convertir en DTOs ---
        Page<ProspectDTO> prospectDTOPage = prospectsPage.map(this::convertToDTO);
        return ProspectPaginatedResponseDTO.fromPage(prospectDTOPage);
    }

    /**
     * Mettre à jour un prospect
     */
    public ProspectDTO updateProspect(Long id, UpdateProspectRequestDTO request, HttpServletRequest httpRequest) {
        // --- 1. Extraction et validation du token JWT ---
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        // --- 2. Vérification des permissions ---
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour modifier ce prospect");
        }

        // --- 3. Récupérer le prospect avec vérification d'appartenance ---
        Prospect prospect = prospectRepository.findByIdAndEntrepriseId(id, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + id));

        // --- 4. Validation des données ---
        if (request.getType() == null) {
            throw new IllegalArgumentException("Le type de prospect est obligatoire (PARTICULIER ou ENTREPRISE)");
        }

        // Validation selon le type
        if (request.getType() == ProspectType.ENTREPRISE) {
            if (request.getNom() == null || request.getNom().trim().isEmpty()) {
                throw new IllegalArgumentException("Le nom de l'entreprise est obligatoire pour un prospect ENTREPRISE");
            }
        } else if (request.getType() == ProspectType.PARTICULIER) {
            if (request.getNomComplet() == null || request.getNomComplet().trim().isEmpty()) {
                throw new IllegalArgumentException("Le prénom est obligatoire pour un prospect PARTICULIER");
            }
            
        }

        // --- 5. Vérifier si un autre prospect avec le même email existe déjà dans l'entreprise ---
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            Optional<Prospect> existingProspect = prospectRepository.findByEmailAndEntrepriseId(request.getEmail(), entreprise.getId());
            if (existingProspect.isPresent() && !existingProspect.get().getId().equals(id)) {
                throw new IllegalArgumentException("Un autre prospect avec cet email existe déjà dans votre entreprise");
            }
        }

        // --- 6. Mettre à jour les champs ---
        prospect.setType(request.getType());
        
        // Remplir les champs selon le type
        if (request.getType() == ProspectType.ENTREPRISE) {
            prospect.setNom(request.getNom().trim());
            prospect.setSector(request.getSector() != null ? request.getSector().trim() : null);
            prospect.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
            prospect.setCity(request.getCity() != null ? request.getCity().trim() : null);
            prospect.setCountry(request.getCountry() != null ? request.getCountry().trim() : null);
            // Vider les champs particulier
            prospect.setNomComplet(null);
            prospect.setAdresse(null);
            prospect.setPays(null);
        } else if (request.getType() == ProspectType.PARTICULIER) {
            prospect.setNomComplet(request.getNomComplet().trim());
            prospect.setAdresse(request.getAdresse() != null ? request.getAdresse().trim() : null);
            prospect.setPays(request.getPays() != null ? request.getPays().trim() : null);
            // Vider les champs entreprise
            prospect.setNom(null);
            prospect.setSector(null);
            prospect.setAddress(null);
            prospect.setCity(null);
            prospect.setCountry(null);
        }
        
        // Champs communs
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
        // --- 1. Extraction et validation du token JWT ---
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        // --- 2. Vérification des permissions (seuls les admins peuvent supprimer) ---
        boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entreprise.getId());
        
        if (!isAdmin) {
            throw new RuntimeException("Accès refusé : Seuls les administrateurs peuvent supprimer un prospect");
        }

        // --- 3. Récupérer le prospect avec vérification d'appartenance ---
        Prospect prospect = prospectRepository.findByIdAndEntrepriseId(id, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + id));

        prospectRepository.delete(prospect);
    }

    /**
     * Ajouter une interaction à un prospect
     */
    public InteractionDTO addInteraction(Long prospectId, CreateInteractionRequestDTO request, HttpServletRequest httpRequest) {
        // --- 1. Extraction et validation du token JWT ---
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        // --- 2. Vérification des permissions ---
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour ajouter une interaction");
        }

        // --- 3. Récupérer le prospect avec vérification d'appartenance ---
        Prospect prospect = prospectRepository.findByIdAndEntrepriseId(prospectId, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + prospectId));

        // --- 4. Validation des données ---
        if (request.getType() == null) {
            throw new IllegalArgumentException("Le type d'interaction est obligatoire");
        }
        if (request.getAssignedTo() == null || request.getAssignedTo().trim().isEmpty()) {
            throw new IllegalArgumentException("L'assigné est obligatoire");
        }

        // --- 5. Créer l'interaction ---
        Interaction interaction = new Interaction();
        interaction.setType(request.getType());
        interaction.setNotes(request.getNotes());
        interaction.setAssignedTo(request.getAssignedTo().trim());
        interaction.setNextFollowUp(request.getNextFollowUp());
        interaction.setProspect(prospect);

        Interaction savedInteraction = interactionRepository.save(interaction);
        return convertToDTO(savedInteraction);
    }

    /**
     * Récupérer les interactions d'un prospect avec pagination
     */
    public List<InteractionDTO> getProspectInteractions(Long prospectId, HttpServletRequest httpRequest) {
        // --- 1. Extraction et validation du token JWT ---
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        // --- 2. Vérification des permissions ---
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_MARKETING);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour voir les interactions");
        }

        // --- 3. Vérifier que le prospect existe et appartient à l'entreprise ---
        prospectRepository.findByIdAndEntrepriseId(prospectId, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + prospectId));

        // --- 4. Récupérer les interactions triées par date décroissante ---
        List<Interaction> interactions = interactionRepository.findByProspectIdOrderByOccurredAtDesc(prospectId);

        return interactions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Supprimer une interaction
     */
    public void deleteInteraction(Long interactionId, HttpServletRequest httpRequest) {
        // --- 1. Extraction et validation du token JWT ---
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        // --- 2. Vérification des permissions (seuls les admins peuvent supprimer) ---
        boolean isAdmin = CentralAccess.isAdminOfEntreprise(user, entreprise.getId());
        
        if (!isAdmin) {
            throw new RuntimeException("Accès refusé : Seuls les administrateurs peuvent supprimer une interaction");
        }

        // --- 3. Récupérer l'interaction avec vérification d'appartenance ---
        Interaction interaction = interactionRepository.findByIdAndProspectEntrepriseId(interactionId, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Interaction non trouvée avec l'ID: " + interactionId));

        interactionRepository.delete(interaction);
    }

    /**
     * Convertir un prospect en client (après achat)
     */
    public Map<String, Object> convertProspectToClient(Long prospectId, ConvertProspectRequestDTO conversionRequest, HttpServletRequest httpRequest) {
        Map<String, Object> response = new HashMap<>();
        
        // --- 1. Extraction et validation du token JWT ---
        String token = httpRequest.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        User user = authHelper.getAuthenticatedUserWithFallback(httpRequest);
        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("L'utilisateur n'est associé à aucune entreprise");
        }

        // --- 2. Vérification des permissions ---
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_CLIENTS);
        
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : Vous n'avez pas les permissions nécessaires pour convertir un prospect en client");
        }

        // --- 3. Récupérer le prospect avec vérification d'appartenance ---
        Prospect prospect = prospectRepository.findByIdAndEntrepriseId(prospectId, entreprise.getId())
                .orElseThrow(() -> new IllegalArgumentException("Prospect non trouvé avec l'ID: " + prospectId));

        // --- 4. Vérifier le produit/service acheté ---
        if (conversionRequest.getProduitId() == null) {
            throw new IllegalArgumentException("L'ID du produit/service acheté est obligatoire");
        }
        
        Produit produit = produitRepository.findById(conversionRequest.getProduitId())
                .orElseThrow(() -> new IllegalArgumentException("Produit/service non trouvé avec l'ID: " + conversionRequest.getProduitId()));
        
        // Vérifier que le produit appartient à l'entreprise
        if (!produit.getBoutique().getEntreprise().getId().equals(entreprise.getId())) {
            throw new IllegalArgumentException("Ce produit/service n'appartient pas à votre entreprise");
        }
        
        // Vérifier que le produit est en stock (pour les produits physiques)
        if (produit.getTypeProduit().name().equals("PHYSIQUE") && !produit.getEnStock()) {
            throw new IllegalArgumentException("Le produit physique '" + produit.getNom() + "' n'est pas en stock");
        }
        
        // Utiliser le montant fourni ou le prix de vente du produit
        Double montantFinal = conversionRequest.getMontantAchat() != null ? 
            conversionRequest.getMontantAchat() : produit.getPrixVente();

        // --- 5. Vérifier si le prospect n'est pas déjà converti ---
        if (prospect.getEmail() != null) {
            Optional<Client> existingClient = clientRepository.findByEmail(prospect.getEmail());
            if (existingClient.isPresent()) {
                throw new IllegalArgumentException("Ce prospect a déjà été converti en client");
            }
        }

        // --- 6. Conversion selon le type ---
        if (prospect.getType() == ProspectType.ENTREPRISE) {
            // Convertir directement en EntrepriseClient (pas de Client associé)
            EntrepriseClient entrepriseClient = new EntrepriseClient();
            entrepriseClient.setNom(prospect.getNom());
            entrepriseClient.setEmail(prospect.getEmail());
            entrepriseClient.setTelephone(prospect.getTelephone());
            entrepriseClient.setAdresse(prospect.getAddress());
            entrepriseClient.setPays(prospect.getCountry());
            entrepriseClient.setSecteur(prospect.getSector());
            entrepriseClient.setEntreprise(entreprise);
            entrepriseClient.setCreatedAt(LocalDateTime.now());

            EntrepriseClient savedEntrepriseClient = entrepriseClientRepository.save(entrepriseClient);
            
            response.put("message", "Prospect ENTREPRISE converti en EntrepriseClient avec succès");
            response.put("entrepriseClientId", savedEntrepriseClient.getId());
            response.put("type", "ENTREPRISE");
            
            // Informations complètes sur le produit/service acheté
            response.put("produitAchete", produit.getNom());
            response.put("produitId", produit.getId());
            response.put("typeProduit", produit.getTypeProduit().name());
            response.put("descriptionProduit", produit.getDescription());
            response.put("prixProduit", produit.getPrixVente());
            response.put("montantAchat", montantFinal);
            response.put("notesAchat", conversionRequest.getNotesAchat());
            response.put("dateAchat", LocalDateTime.now().toString());
            
        } else if (prospect.getType() == ProspectType.PARTICULIER) {
            // Convertir en Client particulier
            Client client = new Client();
            client.setNomComplet(prospect.getNomComplet());
            client.setEmail(prospect.getEmail());
            client.setTelephone(prospect.getTelephone());
            client.setAdresse(prospect.getAdresse());
            client.setPays(prospect.getPays());
            client.setEntreprise(entreprise);
            client.setCreatedAt(LocalDateTime.now());

            Client savedClient = clientRepository.save(client);
            
            response.put("message", "Prospect PARTICULIER converti en Client avec succès");
            response.put("clientId", savedClient.getId());
            response.put("type", "PARTICULIER");
            
            // Informations complètes sur le produit/service acheté
            response.put("produitAchete", produit.getNom());
            response.put("produitId", produit.getId());
            response.put("typeProduit", produit.getTypeProduit().name());
            response.put("descriptionProduit", produit.getDescription());
            response.put("prixProduit", produit.getPrixVente());
            response.put("montantAchat", montantFinal);
            response.put("notesAchat", conversionRequest.getNotesAchat());
            response.put("dateAchat", LocalDateTime.now().toString());
        }

        // --- 7. Supprimer le prospect (optionnel - vous pouvez aussi le marquer comme converti) ---
        // Option 1: Supprimer complètement
        prospectRepository.delete(prospect);
        
        // Option 2: Marquer comme converti (si vous voulez garder l'historique)
        // prospect.setConvertedToClient(true);
        // prospect.setConvertedAt(LocalDateTime.now());
        // prospectRepository.save(prospect);

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
        dto.sector = prospect.getSector();
        dto.address = prospect.getAddress();
        dto.city = prospect.getCity();
        dto.country = prospect.getCountry();
        
        // Champs pour PARTICULIER
        dto.nomComplet = prospect.getNomComplet();
        dto.adresse = prospect.getAdresse();
        dto.pays = prospect.getPays();
      
        
        // Champs communs
        dto.email = prospect.getEmail();
        dto.phone = prospect.getTelephone();
        dto.notes = prospect.getNotes();
        dto.createdAt = prospect.getCreatedAt();

        // Convertir les interactions
        if (prospect.getInteractions() != null) {
            List<InteractionDTO> interactionDTOs = prospect.getInteractions().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            dto.interactions = interactionDTOs;
        }

        return dto;
    }

    /**
     * Convertir une entité Interaction en DTO
     */
    private InteractionDTO convertToDTO(Interaction interaction) {
        InteractionDTO dto = new InteractionDTO();
        dto.id = interaction.getId();
        dto.type = interaction.getType();
        dto.occurredAt = interaction.getOccurredAt();
        dto.notes = interaction.getNotes();
        dto.assignedTo = interaction.getAssignedTo();
        dto.nextFollowUp = interaction.getNextFollowUp();
        return dto;
    }
}
