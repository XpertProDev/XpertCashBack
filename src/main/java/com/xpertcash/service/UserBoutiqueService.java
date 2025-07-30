package com.xpertcash.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xpertcash.DTOs.Boutique.BoutiqueResponse;
import com.xpertcash.DTOs.Boutique.BoutiqueResponseVendeur;
import com.xpertcash.DTOs.Boutique.VendeurDTO;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Role;
import com.xpertcash.entity.User;
import com.xpertcash.entity.UserBoutique;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.UserBoutiqueRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class UserBoutiqueService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private UserBoutiqueRepository userBoutiqueRepository;

    @Autowired
    private JwtUtil jwtUtil;  

    @Autowired
    private RoleService roleService;

@Transactional
public List<String> assignerVendeurAuxBoutiques(HttpServletRequest request, Long userId, List<Long> boutiqueIds) {
    List<String> resultMessages = new ArrayList<>();

    // 🔐 Vérification du token JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long adminId;
    try {
        adminId = jwtUtil.extractUserId(token.substring(7));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token", e);
    }

    // Récupérer l'utilisateur connecté (admin ou manager)
    User admin = usersRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    if (admin.getEntreprise() == null) {
        throw new RuntimeException("L'utilisateur connecté n'appartient à aucune entreprise.");
    }

    Long entrepriseIdAdmin = admin.getEntreprise().getId();

    RoleType role = admin.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    if (!isAdminOrManager) {
        throw new RuntimeException("Vous n'avez pas les droits pour affecter des utilisateurs aux boutiques.");
    }

    // Vérifie si l'utilisateur existe
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    // Vérifier que les boutiques appartiennent à la même entreprise
    List<Boutique> boutiques = boutiqueRepository.findAllById(boutiqueIds);
    if (boutiques.size() != boutiqueIds.size()) {
        throw new RuntimeException("Certaines boutiques n'ont pas été trouvées.");
    }

    for (Boutique boutique : boutiques) {
        if (!boutique.getEntreprise().getId().equals(entrepriseIdAdmin)) {
            throw new RuntimeException("La boutique " + boutique.getNomBoutique() + " n'appartient pas à votre entreprise.");
        }
    }


        // ✅ [NOUVEAU] Assigner le rôle VENDEUR si nécessaire
    if (user.getRole() == null || user.getRole().getName() != RoleType.VENDEUR) {
        Role vendeurRole = roleService.getOrCreateVendeurRole();
        user.setRole(vendeurRole);
        usersRepository.save(user);
        resultMessages.add("Rôle VENDEUR attribué à l'utilisateur.");
    }

    // Assigner l'utilisateur aux boutiques
    for (Boutique boutique : boutiques) {
        Optional<UserBoutique> existingAssignment = userBoutiqueRepository
                .findByUserIdAndBoutiqueId(userId, boutique.getId());

        if (existingAssignment.isPresent()) {
            // L'utilisateur est déjà affecté à la boutique, on l'ajoute à la liste des messages
            resultMessages.add("Utilisateur déjà affecté à la boutique : " + boutique.getNomBoutique());
            continue;
        }

        UserBoutique userBoutique = new UserBoutique();
        userBoutique.setUser(user);
        userBoutique.setBoutique(boutique);
        userBoutique.setAssignedAt(LocalDateTime.now());
        userBoutiqueRepository.save(userBoutique);
        resultMessages.add("Utilisateur affecté à la boutique : " + boutique.getNomBoutique());
    }


    return resultMessages;
}


@Transactional
public List<String> retirerVendeurDesBoutiques(HttpServletRequest request, Long userId, List<Long> boutiqueIds) {
    List<String> resultMessages = new ArrayList<>();

    // 🔐 Vérification du token JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    Long adminId;
    try {
        adminId = jwtUtil.extractUserId(token.substring(7));
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token", e);
    }

    // Récupérer l'utilisateur connecté (admin ou manager)
    User admin = usersRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    if (admin.getEntreprise() == null) {
        throw new RuntimeException("L'utilisateur connecté n'appartient à aucune entreprise.");
    }

    // Récupérer l'entreprise de l'admin (qui doit être la même pour les boutiques)
    Long entrepriseIdAdmin = admin.getEntreprise().getId();

    // Vérification des droits d'accès (Admin ou Manager)
    RoleType role = admin.getRole().getName();
    boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
    if (!isAdminOrManager) {
        throw new RuntimeException("Vous n'avez pas les droits pour retirer des utilisateurs des boutiques.");
    }

    // Vérifie si l'utilisateur existe
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    // Vérifier que l'utilisateur et les boutiques appartiennent à la même entreprise
    List<Boutique> boutiques = boutiqueRepository.findAllById(boutiqueIds);
    if (boutiques.size() != boutiqueIds.size()) {
        throw new RuntimeException("Certaines boutiques n'ont pas été trouvées.");
    }

    // Vérifier que toutes les boutiques appartiennent à l'entreprise de l'admin
    for (Boutique boutique : boutiques) {
        if (!boutique.getEntreprise().getId().equals(entrepriseIdAdmin)) {
            throw new RuntimeException("La boutique " + boutique.getNomBoutique() + " n'appartient pas à votre entreprise.");
        }
    }

    // Retirer l'utilisateur de chaque boutique
    for (Boutique boutique : boutiques) {
        Optional<UserBoutique> existingAssignment = userBoutiqueRepository
                .findByUserIdAndBoutiqueId(userId, boutique.getId());

        if (existingAssignment.isPresent()) {
            // Si l'affectation existe, on la supprime
            userBoutiqueRepository.delete(existingAssignment.get());
            resultMessages.add("Utilisateur retiré de la boutique : " + boutique.getNomBoutique());
        } else {
            resultMessages.add("L'utilisateur n'est pas affecté à la boutique : " + boutique.getNomBoutique());
        }
    }

    return resultMessages; // Renvoie la liste des messages
}


    // Methode pour récupérer toutes les boutiques d'un utilisateur
    @Transactional
    public ResponseEntity<Map<String, Object>> getBoutiquesParUtilisateur(Long userId, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 🔐 Vérification du token JWT
            String token = request.getHeader("Authorization");
            if (token == null || !token.startsWith("Bearer ")) {
                throw new RuntimeException("Token JWT manquant ou mal formaté");
            }

            Long adminId;
            try {
                adminId = jwtUtil.extractUserId(token.substring(7));
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token", e);
            }

            // Récupérer l'utilisateur connecté (admin ou manager)
            User admin = usersRepository.findById(adminId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            if (admin.getEntreprise() == null) {
                throw new RuntimeException("L'utilisateur connecté n'appartient à aucune entreprise.");
            }

            // Vérification des droits d'accès (Admin ou Manager)
            RoleType role = admin.getRole().getName();
            boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
            if (!isAdminOrManager) {
                throw new RuntimeException("Vous n'avez pas les droits pour consulter les boutiques de cet utilisateur.");
            }

            // Vérifie si l'utilisateur existe
            User user = usersRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Vérifier que l'utilisateur appartient à une entreprise
            if (user.getEntreprise() == null) {
                throw new RuntimeException("L'utilisateur n'appartient à aucune entreprise.");
            }

            // Récupère les affectations de boutiques pour cet utilisateur
            List<UserBoutique> userBoutiques = userBoutiqueRepository.findByUserId(userId);

            if (userBoutiques.isEmpty()) {
                throw new RuntimeException("Cet utilisateur n'est affecté à aucune boutique.");
            }

            // Récupère les boutiques associées à l'utilisateur avec la date d'affectation
            List<BoutiqueResponseVendeur> boutiqueResponses = new ArrayList<>();
            for (UserBoutique userBoutique : userBoutiques) {
                Boutique boutique = userBoutique.getBoutique();

                // Vérification si la boutique appartient à la même entreprise que l'utilisateur
                if (!boutique.getEntreprise().getId().equals(user.getEntreprise().getId())) {
                    throw new RuntimeException("La boutique " + boutique.getNomBoutique() + " n'appartient pas à l'entreprise de l'utilisateur.");
                }

                BoutiqueResponseVendeur boutiqueUserResponse = new BoutiqueResponseVendeur(
                        boutique.getId(),
                        boutique.getNomBoutique(),
                        boutique.getAdresse(),
                        boutique.getTypeBoutique(),
                        userBoutique.getAssignedAt()
                );

                boutiqueResponses.add(boutiqueUserResponse);
            }

            response.put("status", "success");
            response.put("boutiques", boutiqueResponses);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // En cas d'erreur, renvoyer une réponse détaillée
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


   // Methode pour recuperer tous les utilisateur dune boutique
    @Transactional
    public List<VendeurDTO> getVendeursDeBoutique(Long boutiqueId, HttpServletRequest request) {
        // 🔐 Vérification du token JWT et des droits d'accès
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        Long adminId;
        try {
            adminId = jwtUtil.extractUserId(token.substring(7));  // Extraction de l'ID de l'utilisateur connecté
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token", e);
        }

        // Récupération de l'utilisateur connecté (admin ou manager)
        User admin = usersRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (admin.getEntreprise() == null) {
            throw new RuntimeException("L'utilisateur connecté n'appartient à aucune entreprise.");
        }

        // Vérification des droits d'accès de l'utilisateur connecté
        RoleType role = admin.getRole().getName();
        boolean isAdminOrManager = role == RoleType.ADMIN || role == RoleType.MANAGER;
        if (!isAdminOrManager) {
            throw new RuntimeException("Vous n'avez pas les droits pour récupérer les vendeurs.");
        }

        // Vérification que la boutique appartient à l'entreprise de l'admin
        Boutique boutique = boutiqueRepository.findById(boutiqueId)
                .orElseThrow(() -> new RuntimeException("La boutique n'a pas été trouvée"));

        if (!boutique.getEntreprise().getId().equals(admin.getEntreprise().getId())) {
            throw new RuntimeException("Cette boutique n'appartient pas à votre entreprise.");
        }

        // Récupérer les vendeurs affectés à cette boutique
        List<UserBoutique> userBoutiques = userBoutiqueRepository.findByBoutiqueId(boutiqueId);

        // Mapper les utilisateurs à des objets VendeurDTO
        List<VendeurDTO> vendeursDTO = userBoutiques.stream().map(userBoutique -> {
            User user = userBoutique.getUser();
            LocalDateTime assignedAt = userBoutique.getAssignedAt();
            return new VendeurDTO(
                    user.getId(),
                    user.getNomComplet(),
                    user.getEmail(),
                    user.getPhone(),
                    user.getPays(),
                    user.getPhoto(),
                    assignedAt
            );
        }).collect(Collectors.toList());

        return vendeursDTO;
    }

}
