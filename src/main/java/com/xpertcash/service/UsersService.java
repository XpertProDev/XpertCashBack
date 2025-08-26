package com.xpertcash.service;

import com.xpertcash.DTOs.EntrepriseDTO;
import com.xpertcash.DTOs.UpdateUserRequest;
import com.xpertcash.DTOs.Boutique.BoutiqueResponse;
import com.xpertcash.DTOs.USER.PermissionDTO;
import com.xpertcash.DTOs.USER.RegisterResponse;
import com.xpertcash.DTOs.USER.RoleDTO;
import com.xpertcash.DTOs.USER.UserDTO;
import com.xpertcash.DTOs.USER.UserRequest;
import com.xpertcash.configuration.CentralAccess;
import com.xpertcash.configuration.JwtConfig;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.configuration.PasswordGenerator;
import com.xpertcash.configuration.QRCodeGenerator;
import com.xpertcash.entity.*;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.TypeBoutique;
import com.xpertcash.entity.Module.AppModule;
import com.xpertcash.exceptions.BusinessException;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.PermissionRepository;
import com.xpertcash.repository.RoleRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.repository.Module.ModuleRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;
import com.xpertcash.service.Module.ModuleActivationService;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;



@Service
public class UsersService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private MailService mailService;

    @Autowired
    private  RoleRepository roleRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil; 
    private final JwtConfig jwtConfig;
    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private ModuleActivationService moduleActivationService;
   

    @Autowired
    private PermissionRepository permissionRepository;
     @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private FactureProformaRepository factureProformaRepository;

    @Autowired
    public UsersService(UsersRepository usersRepository, JwtConfig jwtConfig, BCryptPasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.jwtConfig = jwtConfig;
        this.passwordEncoder = passwordEncoder;
    }




  
   @Transactional(noRollbackFor = MessagingException.class)
    public RegisterResponse registerUsers(String nomComplet, String email, String password, String phone, String pays, String nomEntreprise, String nomBoutique) {
        RegisterResponse response = new RegisterResponse();

        // Vérification des données déjà existantes
        if (usersRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé. L'utilisateur existe déjà.");
        }
        if (usersRepository.findByPhone(phone).isPresent()) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé. L'utilisateur existe déjà.");
        }
        if (entrepriseRepository.findByNomEntreprise(nomEntreprise).isPresent()) {
            throw new RuntimeException("Le nom de l'entreprise est déjà utilisé. L'entreprise existe déjà.");
        }
    
        // Génération du mot de passe haché
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(password);

       // Générer un code PIN de 4 chiffres unique pour la connexion future
       String personalCode;
       boolean isUnique;
       do {
           personalCode = String.format("%04d", new Random().nextInt(10000));  // 4 chiffres (0000 à 9999)
           isUnique = !usersRepository.existsByPersonalCode(personalCode);  // Vérifier si le code PIN existe déjà dans la base de données
       } while (!isUnique);  // Répéter jusqu'à ce qu'un code unique soit généré


    
        // Générer le code PIN d'activation
        String activationCode = String.format("%04d", new Random().nextInt(10000));
    

    
        // Si l'email est bien envoyé, on continue l'enregistrement
    
        // Créer l’entreprise
        String identifiantUnique;
        do {
            identifiantUnique = Entreprise.generateIdentifiantEntreprise();
        } while (entrepriseRepository.existsByIdentifiantEntreprise(identifiantUnique));
    
        Entreprise entreprise = new Entreprise();
        entreprise.setNomEntreprise(nomEntreprise);
        entreprise.setIdentifiantEntreprise(identifiantUnique);
        entreprise.setCreatedAt(LocalDateTime.now());
        entreprise.setLogo("");
        entreprise.setAdresse("");
        entreprise.setSiege("Ville");
        entreprise.setNina("");
        entreprise.setNif("");
        entreprise.setBanque("");
        entreprise.setEmail("");
        entreprise.setTelephone("");
        entreprise.setPays("");
        entreprise.setSecteur("");
        entreprise.setRccm("");
        entreprise.setSignataireNom("Fournisseur");
        entreprise.setSignaturNum("");
        entreprise.setCachetNum("");
        entreprise.setSiteWeb("");
        entreprise.setPrefixe(null);
        entreprise.setSuffixe(null);
        entreprise.setTauxTva(null);

            // Affecter les modules actifs par défaut
        Set<AppModule> modulesParDefaut = new HashSet<>(moduleRepository.findByActifParDefautTrue());
        entreprise.setModulesActifs(modulesParDefaut);

        // Date fin d'essai globale
        entreprise.setDateFinEssaiModulesPayants(LocalDateTime.now().plusDays(30));

        // Sauvegarde entreprise (obligatoire pour générer ID)
        entreprise = entrepriseRepository.save(entreprise);

        // Initialiser essais par module (saveAll optimisé)
        moduleActivationService.initialiserEssaisModulesPayants(entreprise);
        
        entreprise = entrepriseRepository.save(entreprise);

        // Vérifier et attribuer un nom par défaut à la boutique
        if (nomBoutique == null || nomBoutique.trim().isEmpty()) {
            nomBoutique = "Ma Boutique";
        }
    
        // Créer la boutique associée à l'entreprise
        Boutique boutique = new Boutique();
        boutique.setNomBoutique(nomBoutique);
        boutique.setEntreprise(entreprise);
        boutique.setTelephone(phone);
        boutique.setEmail(email);
        boutique.setCreatedAt(LocalDateTime.now());
        boutique.setTypeBoutique(TypeBoutique.BOUTIQUE);
        boutiqueRepository.save(boutique);
    
        // Créer un stock vide initial
       
    
        // Attribution du rôle ADMIN à l’utilisateur
        Role adminRole = roleRepository.findByName(RoleType.ADMIN)
                .orElseThrow(() -> new RuntimeException("Rôle ADMIN non trouvé"));
    
        // Créer l'utilisateur
        User user = new User();
        user.setEmail(email);
        user.setPassword(hashedPassword);
        user.setPhone(phone);
        user.setPays(pays);
        user.setNomComplet(nomComplet);
        user.setEntreprise(entreprise);
        user.setRole(adminRole);
        user.setActivationCode(activationCode);
        user.setPersonalCode(personalCode);
        user.setCreatedAt(LocalDateTime.now());
        user.setActivatedLien(false);
        user.setEnabledLien(true);
        user.setLastActivity(LocalDateTime.now());
        user.setLocked(false);
        
        // génération du QR Code 
        try {
        // Contenu du QR code = juste le personalCode
        String qrContent = personalCode;

        byte[] qrCodeBytes = QRCodeGenerator.generateQRCode(qrContent, 200, 200);

         String fileName = UUID.randomUUID().toString();

        String qrCodeUrl = imageStorageService.saveQrCodeImage(qrCodeBytes, fileName);

        user.setQrCodeUrl(qrCodeUrl);

    } catch (Exception e) {
        System.err.println("Erreur génération QR Code: " + e.getMessage());
    }


   


        usersRepository.save(user);
    
        // Assigner l'utilisateur admin à l'entreprise
        entreprise.setAdmin(user);
        entrepriseRepository.save(entreprise);

         // Essayer d'envoyer l'email, mais ne pas rollback si ça échoue
        try {
            mailService.sendActivationLinkEmail(email, activationCode, personalCode);
            response.setSuccess(true);
            response.setMessage("Compte créé avec succès. Un email d’activation vous a été envoyé.");
        } catch (Exception e) { 
            System.err.println("⚠️ Erreur lors de l'envoi de l'email : " + e.getMessage());
            response.setSuccess(true);
            response.setMessage("Compte créé avec succès. Un email d’activation vous a été envoyé.");

        }


        response.setUser(user);
        return response;

    }

    //Admin name
    public String getNomCompletAdminDeEntreprise(Long entrepriseId) {
        // Récupérer l'entreprise par ID
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));

        // Vérifier si l'entreprise a un administrateur
        User admin = entreprise.getAdmin();
        if (admin != null) {
            return admin.getNomComplet();  // Récupérer le nom complet de l'administrateur
        } else {
            throw new RuntimeException("Aucun administrateur assigné à cette entreprise.");
        }
    }

    // Connexion : permet la connexion même si le compte n'est pas activé
     public Map<String, String> login(String email, String password) {
    User user = usersRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

    if (!passwordEncoder.matches(password, user.getPassword())) {
        throw new RuntimeException("Email ou mot de passe incorrect");
    }

    if (user.isLocked()) {
        throw new RuntimeException("Votre compte est verrouillé pour inactivité.");
    }

    User admin = user.getEntreprise().getAdmin();
    boolean isAdmin = user.getRole().getName().equals(RoleType.ADMIN);
    boolean within24Hours = LocalDateTime.now().isBefore(user.getCreatedAt().plusHours(24));

    user.setLastActivity(LocalDateTime.now());
    usersRepository.save(user);

    String accessToken = generateAccessToken(user, admin, within24Hours);
    String refreshToken = generateRefreshToken(user);

    Map<String, String> tokens = new HashMap<>();
    tokens.put("accessToken", accessToken);
    tokens.put("refreshToken", refreshToken);
    return tokens;
}

            // Génération du token avec infos supplémentaires
            public String generateAccessToken(User user, User admin, boolean within24Hours) {
            long expirationTime = 1000 * 60 * 60 * 24;
            Date now = new Date();
            Date expirationDate = new Date(now.getTime() + expirationTime);

            boolean isAdmin = user.getRole().getName().equals(RoleType.ADMIN);

            boolean userActivated = user.isEnabledLien();
            boolean adminActivated = admin.isEnabledLien();
            boolean userActivationPossible = isAdmin ? (user.isActivatedLien() || within24Hours) : true;

            return Jwts.builder()
                    .setSubject(String.valueOf(user.getId()))
                    .claim("role", user.getRole().getName())
                    .claim("userActivated", userActivated)
                    .claim("adminActivated", adminActivated)
                    .claim("userActivationPossible", userActivationPossible)
                    .setIssuedAt(now)
                    .setExpiration(expirationDate)
                    .signWith(SignatureAlgorithm.HS256, jwtConfig.getSecretKey())
                    .compact();
        }
            public String generateRefreshToken(User user) {
            long refreshExpiration = 1000L * 60 * 60 * 24 * 2;
            Date now = new Date();
            Date expirationDate = new Date(now.getTime() + refreshExpiration);

            return Jwts.builder()
                    .setSubject(String.valueOf(user.getId()))
                    .setIssuedAt(now)
                    .setExpiration(expirationDate)
                    .signWith(SignatureAlgorithm.HS256, jwtConfig.getSecretKey())
                    .compact();
        }


    //Resent Mail
    @Transactional
    public void resendActivationEmail(String email) {
        // Vérifier que l'utilisateur existe
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email."));

        // Vérifier si le compte est déjà activé
        if (user.isActivatedLien()) {
            throw new RuntimeException("Ce compte est déjà activé.");
        }

        // Essayer d’envoyer l’email d’activation
        try {
            mailService.sendActivationLinkEmail(user.getEmail(), user.getActivationCode(), user.getPersonalCode());
        } catch (MessagingException e) {
            System.err.println("Erreur lors du renvoi de l'email : " + e.getMessage());
            throw new RuntimeException("Impossible d’envoyer l’email pour le moment.");
        }
    }



    // Activation du compte via le lien d'activation (email + code PIN)
    @Transactional
    public void activateAccount(String email, String code) {
                User user = usersRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

                if (!user.getActivationCode().equals(code)) {
                    throw new RuntimeException("Code d'activation invalide.");
                }

                // Activer le compte de l'utilisateur
                user.setActivatedLien(true);
                user.setEnabledLien(true);
                usersRepository.save(user);

                // Si c'est un ADMIN, activer tous ses employés
                if (user.getRole() != null && user.getRole().getName().equals(RoleType.ADMIN)) {
                    List<User> usersToActivate = usersRepository.findByEntreprise(user.getEntreprise());
                    usersToActivate.forEach(u -> u.setEnabledLien(true));
                    usersRepository.saveAll(usersToActivate);
                }
            }

    // Pour récupérer le statut du compte d'un utilisateur
    public Map<String, Object> getAccountStatus(String email) {
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Map<String, Object> status = new HashMap<>();
        status.put("email", user.getEmail());
        status.put("activated", user.isActivatedLien());
        status.put("locked", user.isLocked());
        status.put("enabled", user.isEnabledLien());

        // Vérification du temps restant avant expiration
        LocalDateTime expirationTime = user.getCreatedAt().plusHours(24);
        long minutesRemaining = ChronoUnit.MINUTES.between(LocalDateTime.now(), expirationTime);

        if (minutesRemaining > 0) {
            status.put("timeRemaining", minutesRemaining + " minutes restantes avant expiration.");
        } else {
            status.put("timeRemaining", "Compte expiré.");
        }

        return status;
    }

    // Déverrouillage du compte en cas d'inactivité de 30 minutes, via le lien de déverrouillage
    public void unlockAccount(String email, String code) {
        User users = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (users.getActivationCode().equals(code)) {
            users.setLocked(false);
            users.setLastActivity(LocalDateTime.now());
            usersRepository.save(users);
        } else {
            throw new RuntimeException("Code de déverrouillage invalide.");
        }
    }

    //Admin addUserToEntreprise
    @Transactional
    public User addUserToEntreprise(HttpServletRequest request, UserRequest userRequest) {
                // Vérifier la présence du token JWT dans l'entête de la requête
                String token = request.getHeader("Authorization");
                if (token == null || !token.startsWith("Bearer ")) {
                    throw new RuntimeException("Token JWT manquant ou mal formaté");
                }

                // Extraire le token sans le "Bearer "
                token = token.replace("Bearer ", "");

                Long adminId = null;
                try {
                    // Décrypter le token pour obtenir l'ID de l'admin
                    adminId = jwtUtil.extractUserId(token);
                } catch (Exception e) {
                    throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'admin depuis le token", e);
                }

                // Récupérer l'admin par l'ID extrait du token
                User admin = usersRepository.findById(adminId)
                        .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

                 // Vérifier que l'utilisateur a une entreprise
                Entreprise entreprise = admin.getEntreprise();
                if (entreprise == null) {
                    throw new BusinessException("Vous n'avez pas d'entreprise associée.");
                }

                  // 🔐 Vérification des droits : admin, manager ou permission gestion personnel
                boolean isAdmin = CentralAccess.isAdminOfEntreprise(admin, entreprise.getId());
                boolean hasPermission = admin.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);

                if (!isAdmin && !hasPermission) {
                    throw new RuntimeException("Accès refusé : seuls les administrateurs, managers ou les utilisateurs autorisés peuvent ajouter un employé.");
                }

                // Vérifier si un utilisateur avec le même email ou téléphone existe déjà
                if (usersRepository.findByEmailAndEntreprise(userRequest.getEmail(), admin.getEntreprise()).isPresent()) {
                    throw new BusinessException("Un utilisateur avec cet email existe déjà dans votre entreprise.");
                }

                if (usersRepository.findByPhoneAndEntreprise(userRequest.getPhone(), admin.getEntreprise()).isPresent()) {
                    throw new BusinessException("Un utilisateur avec ce numéro de téléphone existe déjà dans votre entreprise.");
                } 

                if (usersRepository.findByPhoneAndEntrepriseAndPays(userRequest.getPhone(), admin.getEntreprise(), userRequest.getPays()).isPresent()) {
                    throw new BusinessException("Un utilisateur avec ce numéro de téléphone existe déjà dans votre entreprise.");
                }

                // Vérifier que le rôle spécifié pour le nouvel utilisateur existe
                Role role = roleRepository.findByName(userRequest.getRoleType())
                        .orElseThrow(() -> new RuntimeException("Rôle invalide : " + userRequest.getRoleType()));

                // Générer un mot de passe et l'encoder
                String generatedPassword = PasswordGenerator.generatePassword();
                String encodedPassword = passwordEncoder.encode(generatedPassword);

                // Générer un code PIN de 4 chiffres unique pour la connexion future
                    String personalCode;
                    boolean isUnique;
                    do {
                        personalCode = String.format("%04d", new Random().nextInt(10000));
                        isUnique = !usersRepository.existsByPersonalCode(personalCode);  // Vérifier si le code PIN existe déjà dans la base de données
                    } while (!isUnique);

                // Vérifier que l'ID de la boutique est présent s'il s'agit d'un vendeur
                  /*  Boutique boutique = null;
                    if (userRequest.getBoutiqueId() != null) {
                        boutique = boutiqueRepository.findById(userRequest.getBoutiqueId())
                                .orElseThrow(() -> new BusinessException("Boutique introuvable."));

                        // Vérifier que la boutique appartient à l'entreprise de l'admin
                        if (!boutique.getEntreprise().getId().equals(admin.getEntreprise().getId())) {
                            throw new BusinessException("La boutique sélectionnée n'appartient pas à votre entreprise.");
                        }
                    }
                    */ 
                    
                // Créer un nouvel utilisateur avec l'activation dépendante de l'admin
                User newUser = new User();
                newUser.setEmail(userRequest.getEmail());
                newUser.setPassword(encodedPassword);
                newUser.setNomComplet(userRequest.getNomComplet());
                newUser.setPays(userRequest.getPays());
                newUser.setPhone(userRequest.getPhone());
                newUser.setActivatedLien(admin.isActivatedLien());
                newUser.setEnabledLien(true);
                newUser.setCreatedAt(LocalDateTime.now());
                newUser.setEntreprise(admin.getEntreprise());
                newUser.setPersonalCode(personalCode);
                newUser.setRole(role);
                newUser.setPhoto(null);

                        
                // génération du QR Code 
                        try {
                        String qrContent = personalCode;

                        byte[] qrCodeBytes = QRCodeGenerator.generateQRCode(qrContent, 200, 200);

                        String fileName = UUID.randomUUID().toString();

                        String qrCodeUrl = imageStorageService.saveQrCodeImage(qrCodeBytes, fileName);

                        newUser.setQrCodeUrl(qrCodeUrl);

                    } catch (Exception e) {
                    System.err.println("Erreur génération QR Code: " + e.getMessage());
                }


                // Enregistrer l'utilisateur
                User savedUser = usersRepository.save(newUser);

                // Envoi de l'email avec les identifiants
                try {
                    mailService.sendEmployeEmail(
                        savedUser.getEmail(),
                        savedUser.getNomComplet(),
                        savedUser.getEntreprise().getNomEntreprise(),
                        savedUser.getRole().getName().toString(),
                        savedUser.getEmail(),
                        generatedPassword,
                        savedUser.getPersonalCode()

                    );
                } catch (MessagingException e) {
                    System.err.println("Erreur lors de l'envoi de l'email à " + savedUser.getEmail() + " : " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("Utilisateur créé mais une erreur est survenue lors de l'envoi de l'email.", e);
                }
                return savedUser;
            }

    //Attribution des permissions à un utilisateur
    @Transactional
    public UserDTO assignPermissionsToUser(Long userId, Map<PermissionType, Boolean> permissions, HttpServletRequest request) {
        // 🔐 Extraction du token JWT
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        token = token.replace("Bearer ", "");
        Long currentUserId = jwtUtil.extractUserId(token);

        // 👤 Utilisateur courant
        User currentUser = usersRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur courant non trouvé"));

        // 👤 Utilisateur cible
        User targetUser = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur cible non trouvé"));

        // 🔒 Vérifier qu’ils sont dans la même entreprise
        if (currentUser.getEntreprise() == null || targetUser.getEntreprise() == null ||
            !currentUser.getEntreprise().getId().equals(targetUser.getEntreprise().getId())) {
            throw new RuntimeException("Les utilisateurs doivent appartenir à la même entreprise.");
        }

        // 🚫 Interdiction de modifier les permissions de l'admin
        boolean isTargetAdmin = CentralAccess.isAdminOfEntreprise(targetUser, targetUser.getEntreprise().getId());
        if (isTargetAdmin) {
            throw new RuntimeException("Impossible de modifier les permissions de l'administrateur de l'entreprise.");
        }

        // 🚫 Interdiction de modifier ses propres permissions sauf si ADMIN
        boolean isSelf = currentUser.getId().equals(targetUser.getId());
        boolean isAdmin = CentralAccess.isAdminOfEntreprise(currentUser, currentUser.getEntreprise().getId());
        boolean hasPermission = currentUser.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);

        if (isSelf && !isAdmin) {
            throw new RuntimeException("Vous ne pouvez pas modifier vos propres permissions !");
        }

        if (!isAdmin && !hasPermission) {
            throw new RuntimeException("Accès refusé : seuls les administrateurs ou personnes autorisées peuvent gérer les permissions.");
        }

        // 🎯 Vérification que l’utilisateur cible a bien un rôle
        if (targetUser.getRole() == null) {
            throw new RuntimeException("L'utilisateur cible n'a pas de rôle attribué.");
        }

        // ⚙️ Mise à jour des permissions
        List<Permission> existingPermissions = targetUser.getRole().getPermissions();

        permissions.forEach((permissionType, isEnabled) -> {
            Permission permission = permissionRepository.findByType(permissionType)
                    .orElseThrow(() -> new RuntimeException("Permission non trouvée : " + permissionType));

            if (isEnabled) {
                if (!existingPermissions.contains(permission)) {
                    existingPermissions.add(permission);
                }
            } else {
                existingPermissions.remove(permission);
            }
        });

        // 💾 Sauvegarde du rôle modifié
        roleRepository.save(targetUser.getRole());

        // Récupération des permissions du rôle mis à jour
        List<PermissionDTO> permissionsDTO = targetUser.getRole().getPermissions().stream()
                .map(permission -> new PermissionDTO(permission.getId(), permission.getType().toString()))
                .collect(Collectors.toList());

        // Création du RoleDTO avec les permissions
        RoleDTO roleDTO = new RoleDTO(targetUser.getRole().getId(), targetUser.getRole().getName().toString(), permissionsDTO);

        // Conversion de l'utilisateur en DTO et retour
        return new UserDTO(
            targetUser.getId(),
            targetUser.getPersonalCode(),
            targetUser.getNomComplet(),
            targetUser.getEmail(),
            targetUser.getPhone(),
            targetUser.getPays(),
            targetUser.getPhoto(),
            targetUser.getCreatedAt().toString(),
            targetUser.getActivationCode(),
            targetUser.isActivatedLien(),
            targetUser.isEnabledLien(),
            targetUser.getLastActivity() != null ? targetUser.getLastActivity().toString() : null,
            targetUser.isLocked(),
            roleDTO,
            targetUser.getUserBoutiques().stream().map(UserBoutique::getId).collect(Collectors.toList()) // Adapté si tu veux juste les IDs des userBoutiques
        );
    }

    //Suprim UserToEntreprise 
    @Transactional
    public void deleteUserFromEntreprise(HttpServletRequest request, Long userId) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    token = token.replace("Bearer ", "");

    Long adminId;
    try {
        adminId = jwtUtil.extractUserId(token);
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID de l'admin depuis le token", e);
    }

    User admin = usersRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

    if (admin.getRole() == null) {
        throw new RuntimeException("Rôle de l'utilisateur non défini");
    }

    RoleType role = admin.getRole().getName();
    if (role != RoleType.ADMIN && role != RoleType.MANAGER) {
        throw new RuntimeException("Seuls les utilisateurs avec le rôle ADMIN ou MANAGER peuvent supprimer des utilisateurs.");
    }

    if (admin.getEntreprise() == null) {
        throw new RuntimeException("L'Admin n'a pas d'entreprise associée.");
    }

    User userToDelete = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur à supprimer non trouvé"));

    // Empêcher de se supprimer soi-même
    if (adminId.equals(userId)) {
        throw new RuntimeException("Vous ne pouvez pas supprimer votre propre compte.");
    }

    // Empêcher qu’un manager supprime un admin
    if (role == RoleType.MANAGER && userToDelete.getRole().getName() == RoleType.ADMIN) {
        throw new RuntimeException("Un Manager ne peut pas supprimer un Admin.");
    }

    // Vérifier que l'utilisateur appartient à la même entreprise
    if (!userToDelete.getEntreprise().equals(admin.getEntreprise())) {
        throw new RuntimeException("Vous ne pouvez supprimer que les utilisateurs de votre entreprise.");
    }

    // Vérification des factures liées
    List<FactureProForma> facturesLiees = factureProformaRepository.findByUtilisateurCreateur_Id(userId);
    if (!facturesLiees.isEmpty()) {
        throw new RuntimeException("Impossible de supprimer cet utilisateur : il est lié à des factures.");
    }

    // Suppression du QR Code associé
    if (userToDelete.getQrCodeUrl() != null) {
        try {
            imageStorageService.deleteQrCodeImage(userToDelete.getQrCodeUrl());
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression du QR Code : " + e.getMessage());
            throw new RuntimeException("Impossible de supprimer le QR Code de l'utilisateur.", e);
        }
    }
   

    usersRepository.delete(userToDelete);
}


    // Pour la modification de utilisateur
   @Transactional
    public User updateUser(Long userId, UpdateUserRequest request, MultipartFile imageUserFile, Boolean deletePhoto) {
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

    // Détecter s’il y a une modification sensible
    boolean isSensitiveChange =
            (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) ||
            (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) ||
            (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) ||
            (request.getNomComplet() != null && !request.getNomComplet().equals(user.getNomComplet()));

    // Si modification sensible, vérification du mot de passe
    if (isSensitiveChange) {
        if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect. Modification refusée.");
        }

        // Mise à jour du mot de passe
        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            if (request.getNewPassword().length() < 8) {
                throw new RuntimeException("Le nouveau mot de passe doit contenir au moins 8 caractères.");
            }
            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                throw new RuntimeException("Le nouveau mot de passe ne peut pas être identique à l'ancien.");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        // Vérification et mise à jour du téléphone
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            Optional<User> existingUserWithPhone = usersRepository.findByPhone(request.getPhone());
            if (existingUserWithPhone.isPresent() && !existingUserWithPhone.get().getId().equals(userId)) {
                throw new RuntimeException("Ce numéro de téléphone est déjà utilisé par un autre utilisateur.");
            }
            user.setPhone(request.getPhone());
        }

        // Vérification et mise à jour de l'email
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            Optional<User> existingUserWithEmail = usersRepository.findByEmail(request.getEmail());
            if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getId().equals(userId)) {
                throw new RuntimeException("Cet email est déjà utilisé par un autre utilisateur.");
            }
            user.setEmail(request.getEmail());
        }

        // Mise à jour du nom complet
//        if (request.getNomComplet() != null) {
//            user.setNomComplet(request.getNomComplet());
//        }
    }

    // Mise à jour de la photo si image présente
    if (imageUserFile != null && !imageUserFile.isEmpty()) {
        String oldImagePath = user.getPhoto();
        if (oldImagePath != null && !oldImagePath.isBlank()) {
            Path oldPath = Paths.get("src/main/resources/static" + oldImagePath);
            try {
                Files.deleteIfExists(oldPath);
                System.out.println("🗑️ Ancien photo profile supprimé : " + oldImagePath);
            } catch (IOException e) {
                System.out.println("⚠️ Impossible de supprimer l'ancien photo profile : " + e.getMessage());
            }
        }

        String newImageUrl = imageStorageService.saveUserImage(imageUserFile);
        user.setPhoto(newImageUrl);
        System.out.println("📸 Nouveau logo enregistré : " + newImageUrl);
    }

    System.out.println("📥 DTO reçu dans le controller : " + request);

        if (Boolean.TRUE.equals(deletePhoto)) {
        String oldImagePath = user.getPhoto();
        if (oldImagePath != null && !oldImagePath.isBlank()) {
            Path oldPath = Paths.get("src/main/resources/static" + oldImagePath);
            try {
                Files.deleteIfExists(oldPath);
                System.out.println("🗑️ Ancienne photo supprimée : " + oldImagePath);
            } catch (IOException e) {
                System.out.println("⚠️ Erreur suppression photo : " + e.getMessage());
            }
            user.setPhoto(null);
        }
    }
    usersRepository.save(user);
    return user;
}

    //Get user info
    public UserRequest getInfo(Long userId) {
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Entreprise entreprise = user.getEntreprise();

        List<BoutiqueResponse> boutiqueResponses;
        String roleType = user.getRole().getName().name();

        if (roleType.equals("VENDEUR")) {
            // Boutiques assignées au vendeur
            boutiqueResponses = user.getUserBoutiques()
                    .stream()
                    .map(b -> new BoutiqueResponse(
                            b.getId(),
                            b.getBoutique().getNomBoutique(),
                            b.getBoutique().getAdresse(),
                            b.getBoutique().getTelephone(),
                            b.getBoutique().getEmail(),
                            b.getBoutique().getCreatedAt(),
                            b.getBoutique().isActif(),
                            b.getBoutique().getTypeBoutique()
                    ))
                    .collect(Collectors.toList());
        } else {
            // ADMIN ou MANAGER : toutes les boutiques de l'entreprise
            boutiqueResponses = entreprise.getBoutiques()
                    .stream()
                    .map(b -> new BoutiqueResponse(
                            b.getId(),
                            b.getNomBoutique(),
                            b.getAdresse(),
                            b.getTelephone(),
                            b.getEmail(),
                            b.getCreatedAt(),
                            b.isActif(),
                            b.getTypeBoutique()
                    ))
                    .collect(Collectors.toList());
        }

        // Permissions
        List<String> permissions = user.getRole().getPermissions()
                .stream()
                .map(permission -> permission.getType().name())
                .collect(Collectors.toList());

        return new UserRequest(user, entreprise, boutiqueResponses, permissions);
    }


// Pour la récupération de tous les utilisateurs d'une entreprise
 public List<UserDTO> getAllUsersOfEntreprise(HttpServletRequest request) {
    // Extraction du token JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    // Extraction de l'ID de l'utilisateur à partir du token
    Long userId = jwtUtil.extractUserId(token.replace("Bearer ", ""));
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

    // Récupération de l'entreprise associée à l'utilisateur
    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur.");
    }

    // Vérification des permissions de l'utilisateur
    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);

    // Vérification des droits d'accès
    if (!isAdminOrManager && !hasPermission) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires.");
    }

    // Récupération de tous les utilisateurs de l'entreprise
    List<User> users = usersRepository.findByEntrepriseId(entreprise.getId());

    // Transformation de la liste d'utilisateurs en liste de UserDTO
    return users.stream().map(userEntity -> {
        // Récupérer les permissions du rôle
        List<PermissionDTO> permissionsDTO = userEntity.getRole().getPermissions().stream()
            .map(permission -> new PermissionDTO(permission.getId(), permission.getType().toString()))
            .collect(Collectors.toList());

        // Création du RoleDTO avec les permissions
        RoleDTO roleDTO = new RoleDTO(userEntity.getRole().getId(), userEntity.getRole().getName().toString(), permissionsDTO);

        // Liste simplifiée des ID des UserBoutiques
        List<Long> userBoutiques = userEntity.getUserBoutiques().stream()
                .map(userBoutique -> userBoutique.getId())
                .collect(Collectors.toList());

        // Création du UserDTO
        return new UserDTO(
            userEntity.getId(),
            userEntity.getPersonalCode(),
            userEntity.getNomComplet(),
            userEntity.getEmail(),
            userEntity.getPhone(),
            userEntity.getPays(),
            userEntity.getPhoto(),
            userEntity.getCreatedAt().toString(),
            userEntity.getActivationCode(),
            userEntity.isActivatedLien(),
            userEntity.isEnabledLien(),
            userEntity.getLastActivity() != null ? userEntity.getLastActivity().toString() : null,
            userEntity.isLocked(),
            roleDTO,
            userBoutiques
        );
    }).collect(Collectors.toList());
}


    //Get user by id
  public UserDTO getUserById(Long userId, HttpServletRequest request) {

    // Extraction du token JWT
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    token = token.replace("Bearer ", "");
    Long connectedUserId;
    try {
        connectedUserId = jwtUtil.extractUserId(token);
    } catch (Exception e) {
        throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token", e);
    }

    // Récupération du user connecté
    User connectedUser = usersRepository.findById(connectedUserId)
            .orElseThrow(() -> new RuntimeException("Utilisateur connecté introuvable"));

    // Récupération du user ciblé
    User targetUser = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur demandé introuvable"));

    // Vérification des permissions : gestion des utilisateurs
    boolean hasGestionUtilisateurPermission = targetUser.getRole() != null &&
            targetUser.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);

    // Vérification des droits : admin ou manager de la même entreprise
    RoleType role = connectedUser.getRole().getName();
    boolean isAdminOrManager = (role == RoleType.ADMIN || role == RoleType.MANAGER)
            && connectedUser.getEntreprise() != null
            && targetUser.getEntreprise() != null
            && connectedUser.getEntreprise().getId().equals(targetUser.getEntreprise().getId());

    boolean isSelf = connectedUserId.equals(userId);

    // Vérification des droits d'accès
    if (!isAdminOrManager && !isSelf && !hasGestionUtilisateurPermission) {
        throw new RuntimeException("Accès interdit : vous ne pouvez consulter que vos propres informations !");
    }

    // Récupération des permissions du rôle
    List<PermissionDTO> permissionsDTO = targetUser.getRole().getPermissions().stream()
            .map(permission -> new PermissionDTO(permission.getId(), permission.getType().toString()))
            .collect(Collectors.toList());

    // Création du DTO du rôle avec les permissions
    RoleDTO roleDTO = new RoleDTO(targetUser.getRole().getId(), targetUser.getRole().getName().toString(), permissionsDTO);
    
    // Liste des boutiques de l'utilisateur (par leur ID)
    List<Long> userBoutiquesIds = targetUser.getUserBoutiques().stream()
            .map(userBoutique -> userBoutique.getBoutique().getId())
            .collect(Collectors.toList());

    // Conversion de l'utilisateur en UserDTO
    UserDTO userDTO = new UserDTO(
        targetUser.getId(),
        targetUser.getPersonalCode(),
        targetUser.getNomComplet(),
        targetUser.getEmail(),
        targetUser.getPhone(),
        targetUser.getPays(),
        targetUser.getPhoto(),
        targetUser.getCreatedAt() != null ? targetUser.getCreatedAt().toString() : null,
        targetUser.getActivationCode(),
        targetUser.isActivatedLien(),
        targetUser.isEnabledLien(),
        targetUser.getLastActivity() != null ? targetUser.getLastActivity().toString() : null,
        targetUser.isLocked(),
        roleDTO,
        userBoutiquesIds
    );

    return userDTO;
}

    // Méthode pour suspendre ou réactiver un utilisateur
    @Transactional
    public void suspendUser(HttpServletRequest request, Long userId, boolean suspend) {
        // 🔐 Extraction du token
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        token = token.replace("Bearer ", "");

        Long currentUserId;
        try {
            currentUserId = jwtUtil.extractUserId(token);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'extraction de l'ID utilisateur depuis le token", e);
        }

        // 👤 Utilisateur courant
        User currentUser = usersRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur courant non trouvé"));

        // 👤 Utilisateur cible
        User targetUser = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur cible non trouvé"));

        // 🔐 Vérifier qu’ils sont dans la même entreprise
        if (currentUser.getEntreprise() == null || targetUser.getEntreprise() == null ||
            !currentUser.getEntreprise().getId().equals(targetUser.getEntreprise().getId())) {
            throw new RuntimeException("Les utilisateurs doivent appartenir à la même entreprise.");
        }

        // 🚫 Interdiction de se suspendre soi-même, sauf si ADMIN
        
        boolean isSelf = currentUser.getId().equals(targetUser.getId());
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(currentUser, currentUser.getEntreprise().getId());
        boolean hasPermission = currentUser.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);

        if (isSelf && !isAdminOrManager) {
            throw new RuntimeException("Vous ne pouvez pas vous suspendre vous-même !");
        }

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : seuls les administrateurs ou personnes autorisées peuvent suspendre/réactiver des utilisateurs.");
        }

        // 🚫 Interdiction de suspendre l'admin
        boolean isTargetAdmin = CentralAccess.isAdminOfEntreprise(targetUser, targetUser.getEntreprise().getId());
        if (isTargetAdmin) {
            throw new RuntimeException("Impossible de suspendre l'administrateur de l'entreprise.");
        }

        // ✅ Suspension ou réactivation
        targetUser.setEnabledLien(!suspend);
        usersRepository.save(targetUser);
    }

    // Methode pour nombre de utilisateurs dans l'entreprise
    public int countUsersInEntreprise(HttpServletRequest request) {
        // Extraction du token JWT
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        token = token.replace("Bearer ", "");
        Long userId = jwtUtil.extractUserId(token);

        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur.");
        }

        // Vérification des permissions de l'utilisateur
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);

        // Vérification des droits d'accès
        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires.");
        }

        // Récupération du nombre d'utilisateurs dans l'entreprise
      return (int) usersRepository.countByEntrepriseIdExcludingRole(entreprise.getId(), RoleType.ADMIN);


    }
   

    //Methode qui recupere linformation de lentrprise de user connecter
    public EntrepriseDTO getEntrepriseOfConnectedUser(HttpServletRequest request) {
    String authorizationHeader = request.getHeader("Authorization");

    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    String token = authorizationHeader.substring(7); // Retirer "Bearer "

    Long userId;
    try {
        userId = jwtUtil.extractUserId(token);
    } catch (JwtException e) {
        throw new RuntimeException("Token JWT invalide ou expiré", e);
    }

    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Entreprise associée à l'utilisateur non trouvée");
    }
    // Création et retour du DTO
    EntrepriseDTO dto = new EntrepriseDTO();
    dto.setId(entreprise.getId());
    dto.setNom(entreprise.getNomEntreprise());
    dto.setAdminNom(user.getNomComplet());
    dto.setCreatedAt(entreprise.getCreatedAt());
    dto.setAdresse(entreprise.getAdresse());
    dto.setLogo(entreprise.getLogo());
    dto.setSiege(entreprise.getSiege());
    dto.setNina(entreprise.getNina());
    dto.setNif(entreprise.getNif());
    dto.setBanque(entreprise.getBanque());
    dto.setEmail(entreprise.getEmail());
    dto.setTelephone(entreprise.getTelephone());
    dto.setPays(entreprise.getPays());
    dto.setSecteur(entreprise.getSecteur());
    dto.setRccm(entreprise.getRccm());
    dto.setSiteWeb(entreprise.getSiteWeb());
    dto.setSignataire(entreprise.getSignataire());
    dto.setSignataireNom(entreprise.getSignataireNom());
    dto.setSignaturNum(entreprise.getSignaturNum());
    dto.setCachetNum(entreprise.getCachetNum());
    dto.setTauxTva(entreprise.getTauxTva());
    dto.setPrefixe(entreprise.getPrefixe());
    dto.setSuffixe(entreprise.getSuffixe());


    return dto;
}


}
