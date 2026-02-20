package com.xpertcash.service;

import com.xpertcash.DTOs.EntrepriseDTO;
import com.xpertcash.DTOs.UpdateUserRequest;
import com.xpertcash.DTOs.Boutique.BoutiqueResponse;
import com.xpertcash.DTOs.USER.AssignPermissionsRequest;
import com.xpertcash.DTOs.USER.PermissionDTO;
import com.xpertcash.DTOs.USER.RegisterResponse;
import com.xpertcash.DTOs.USER.RoleDTO;
import com.xpertcash.DTOs.USER.UserBoutiqueDTO;
import com.xpertcash.DTOs.USER.UserDTO;
import com.xpertcash.DTOs.USER.UserRequest;
import com.xpertcash.DTOs.USER.VendeurDTO;
import com.xpertcash.DTOs.UserOptimalDTO;
import com.xpertcash.composant.Utilitaire;
import com.xpertcash.configuration.CentralAccess;
import com.xpertcash.configuration.JwtConfig;
import com.xpertcash.configuration.JwtUtil;

import com.xpertcash.configuration.PasswordGenerator;
import com.xpertcash.configuration.QRCodeGenerator;
import com.xpertcash.entity.*;
import com.xpertcash.entity.Enum.RoleType;
import com.xpertcash.entity.Enum.TypeBoutique;
import com.xpertcash.entity.PermissionType;
import com.xpertcash.entity.Module.AppModule;
import com.xpertcash.entity.UserSession;
import com.xpertcash.entity.PASSWORD.InitialPasswordToken;
import com.xpertcash.exceptions.BusinessException;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.FactureProformaRepository;
import com.xpertcash.repository.PermissionRepository;
import com.xpertcash.repository.RoleRepository;
import com.xpertcash.repository.UserBoutiqueRepository;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.repository.Module.ModuleRepository;
import com.xpertcash.repository.UserSessionRepository;
import com.xpertcash.repository.PASSWORD.InitialPasswordTokenRepository;
import com.xpertcash.service.IMAGES.ImageStorageService;
import com.xpertcash.service.Module.ModuleActivationService;

import io.jsonwebtoken.Claims;
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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

    private static final Map<PermissionType, String> PERMISSION_DESCRIPTIONS = new HashMap<>();
    private static final Map<RoleType, String> ROLE_DESCRIPTIONS = new HashMap<>();
    
    static {
        // Initialisation des descriptions des permissions
        PERMISSION_DESCRIPTIONS.put(PermissionType.GERER_PRODUITS, "Gérer les produits");
        PERMISSION_DESCRIPTIONS.put(PermissionType.VENDRE_PRODUITS, "Vendre des produits");
        PERMISSION_DESCRIPTIONS.put(PermissionType.APPROVISIONNER_STOCK, "Approvisionner le stock");
        PERMISSION_DESCRIPTIONS.put(PermissionType.GESTION_FACTURATION, "Gestion de la facturation");
        PERMISSION_DESCRIPTIONS.put(PermissionType.GERER_CLIENTS, "Gérer les clients");
        PERMISSION_DESCRIPTIONS.put(PermissionType.GERER_FOURNISSEURS, "Gérer les fournisseurs");
        PERMISSION_DESCRIPTIONS.put(PermissionType.GERER_UTILISATEURS, "Gérer les utilisateurs");
        PERMISSION_DESCRIPTIONS.put(PermissionType.GERER_BOUTIQUE, "Gérer les boutiques");
        PERMISSION_DESCRIPTIONS.put(PermissionType.ACTIVER_BOUTIQUE, "Activer les boutiques");
        PERMISSION_DESCRIPTIONS.put(PermissionType.DESACTIVER_BOUTIQUE, "Désactiver les boutiques");
        PERMISSION_DESCRIPTIONS.put(PermissionType.COMPTABILITE, "Comptabilité");
        PERMISSION_DESCRIPTIONS.put(PermissionType.VOIR_FLUX_COMPTABLE, "Voir les flux comptables");
        PERMISSION_DESCRIPTIONS.put(PermissionType.GERER_MARKETING, "Gérer le marketing");
        
        // Initialisation des descriptions des rôles
        ROLE_DESCRIPTIONS.put(RoleType.SUPER_ADMIN, "Super Administrateur");
        ROLE_DESCRIPTIONS.put(RoleType.ADMIN, "Administrateur");
        ROLE_DESCRIPTIONS.put(RoleType.MANAGER, "Gestionnaire");
        ROLE_DESCRIPTIONS.put(RoleType.VENDEUR, "Vendeur");
        ROLE_DESCRIPTIONS.put(RoleType.UTILISATEUR, "Utilisateur");
        ROLE_DESCRIPTIONS.put(RoleType.COMPTABLE, "Comptable");
        ROLE_DESCRIPTIONS.put(RoleType.RH, "Ressources Humaines");
        ROLE_DESCRIPTIONS.put(RoleType.Clientel, "Client");
        ROLE_DESCRIPTIONS.put(RoleType.Fournisseur, "Fournisseur");
        ROLE_DESCRIPTIONS.put(RoleType.GERER_MARKETING, "Gérer le marketing");
    }

    @Autowired
    private AuthenticationHelper authHelper;

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

    private final JwtConfig jwtConfig;


    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private BoutiqueRepository boutiqueRepository;

    @Autowired
    private ModuleActivationService moduleActivationService;
   

    @Autowired
    private PermissionRepository permissionRepository;
    
    @Autowired
    private UserBoutiqueRepository userBoutiqueRepository;
    
    @Autowired
    private ImageStorageService imageStorageService;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private FactureProformaRepository factureProformaRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private DeviceDetectionService deviceDetectionService;

    @Autowired
    private InitialPasswordTokenRepository initialPasswordTokenRepository;

    @Autowired
    public UsersService(UsersRepository usersRepository, JwtConfig jwtConfig, BCryptPasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.jwtConfig = jwtConfig;
        this.passwordEncoder = passwordEncoder;
    }


    @Transactional
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        String token = authHeader.substring(7);

        Claims claims = jwtUtil.extractAllClaimsSafe(token);
        if (claims == null) {
            throw new RuntimeException("Token invalide ou expiré");
        }

        String userUuid = claims.getSubject();
        if (userUuid == null) {
            throw new RuntimeException("UUID utilisateur non trouvé dans le token");
        }

        UserSession session = null;
        Object sessionIdClaim = claims.get("sessionId");
        
        if (sessionIdClaim != null) {
            Long sessionId = ((Number) sessionIdClaim).longValue();
            session = userSessionRepository.findById(sessionId).orElse(null);
        }
        
        if (session == null) {
            session = userSessionRepository.findBySessionToken(token).orElse(null);
        }

        if (session != null) {
            userSessionRepository.delete(session);
        } else {
            
        }
    }



   @Transactional(noRollbackFor = MessagingException.class)
    public RegisterResponse registerUsers(String nomComplet, String email, String password, String phone, String indicatifPays, String pays, String nomEntreprise, String nomBoutique) {
        RegisterResponse response = new RegisterResponse();

        // Enregistrer le numero avec l'indicatif (+223 etc.) : champ indicatif envoye par le front, sinon derive du pays
        if (phone != null && !phone.isBlank() && !phone.trim().startsWith("+")) {
            String indicatif = (indicatifPays != null && !indicatifPays.isBlank())
                    ? (indicatifPays.startsWith("+") ? indicatifPays.trim() : "+" + indicatifPays.trim())
                    : Utilitaire.getIndicatifPays(pays);
            if (indicatif != null) phone = indicatif + " " + phone.trim();
        }

        if (usersRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé.");
        }
        if (usersRepository.findByPhone(phone).isPresent()) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé.");
        }
        if (entrepriseRepository.findByNomEntreprise(nomEntreprise).isPresent()) {
            throw new RuntimeException("Le nom de l'entreprise est déjà utilisé.");
        }
    
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(password);

       String personalCode;
       boolean isUnique;
       do {
           personalCode = String.format("%04d", new Random().nextInt(10000));
           isUnique = !usersRepository.existsByPersonalCode(personalCode);
       } while (!isUnique);

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
        entreprise.setTelephone(phone != null ? phone : "");
        entreprise.setPays(pays != null ? pays : "");
        entreprise.setSecteur("");
        entreprise.setRccm("");
        entreprise.setSignataireNom("Fournisseur");
        entreprise.setSignaturNum("");
        entreprise.setCachetNum("");
        entreprise.setSiteWeb("");
        entreprise.setPrefixe(null);
        entreprise.setSuffixe(null);
        entreprise.setTauxTva(null);

        Set<AppModule> modulesParDefaut = new HashSet<>(moduleRepository.findByActifParDefautTrue());
        entreprise.setModulesActifs(modulesParDefaut);

        entreprise.setDateFinEssaiModulesPayants(LocalDateTime.now().plusDays(30));

        entreprise = entrepriseRepository.save(entreprise);

        moduleActivationService.initialiserEssaisModulesPayants(entreprise);
        
        entreprise = entrepriseRepository.save(entreprise);

        if (nomBoutique == null || nomBoutique.trim().isEmpty()) {
            nomBoutique = "Ma Boutique";
        }
    
        Boutique boutique = new Boutique();
        boutique.setNomBoutique(nomBoutique);
        boutique.setEntreprise(entreprise);
        boutique.setTelephone(phone);
        boutique.setEmail(email);
        boutique.setCreatedAt(LocalDateTime.now());
        boutique.setTypeBoutique(TypeBoutique.BOUTIQUE);
        boutiqueRepository.save(boutique);

            
       
    
        Role adminRole = roleRepository.findFirstByName(RoleType.ADMIN)
                .orElseThrow(() -> new RuntimeException("Rôle ADMIN non trouvé"));
    
        // Créer l'utilisateur
        User user = new User();
        user.setUuid(UUID.randomUUID().toString());
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
        String qrContent = personalCode;

        byte[] qrCodeBytes = QRCodeGenerator.generateQRCode(qrContent, 200, 200);

         String fileName = UUID.randomUUID().toString();

        String qrCodeUrl = imageStorageService.saveQrCodeImage(qrCodeBytes, fileName);

        user.setQrCodeUrl(qrCodeUrl);

        } catch (Exception e) {
            System.err.println("Erreur génération QR Code: " + e.getMessage());
        }


   


        usersRepository.save(user);
    
        entreprise.setAdmin(user);
        entrepriseRepository.save(entreprise);

        try {
            mailService.sendActivationLinkEmail(email, activationCode, personalCode);
            response.setSuccess(true);
            response.setMessage("Compte créé avec succès. Un email d’activation vous a été envoyé.");
        } catch (Exception e) { 
            System.err.println(" Erreur lors de l'envoi de l'email : " + e.getMessage());
            response.setSuccess(true);
            response.setMessage("Compte créé avec succès. Un email d’activation vous a été envoyé.");

        }


        response.setUser(user);
        return response;

    }

    public String getNomCompletAdminDeEntreprise(Long entrepriseId) {
        Entreprise entreprise = entrepriseRepository.findById(entrepriseId)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));

        User admin = entreprise.getAdmin();
        if (admin != null) {
            return admin.getNomComplet();
        } else {
            throw new RuntimeException("Aucun administrateur assigné à cette entreprise.");
        }
    }

    // permet la connexion même si le compte n'est pas activé
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED, 
                   rollbackFor = Exception.class)
    public Map<String, String> login(String email, String password, String deviceId, String deviceName, String ipAddress, String userAgent) {
    User user = usersRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

    Entreprise entreprise = user.getEntreprise();
    if (entreprise != null && Boolean.FALSE.equals(entreprise.getActive())) {
        throw new RuntimeException("Cette entreprise est désactivée. La connexion est impossible.");
    }

    if (!passwordEncoder.matches(password, user.getPassword())) {
        throw new RuntimeException("Email ou mot de passe incorrect");
    }

    if (user.isLocked()) {
        throw new RuntimeException("Votre compte est verrouillé pour inactivité.");
    }


        // Générer deviceId si non fourni
        final String finalDeviceId;
        if (deviceId == null || deviceId.trim().isEmpty()) {
            finalDeviceId = UUID.randomUUID().toString();
        } else {
            finalDeviceId = deviceId;
        }


        UserSession existingSession = userSessionRepository
                .findByDeviceIdAndUserUuidAndIsActiveTrue(finalDeviceId, user.getUuid())
                .orElse(null);

        UserSession session;
        boolean isExistingSession = (existingSession != null);
        
        if (isExistingSession) {
            session = existingSession;
            session.updateLastActivity();
            session.setExpiresAt(LocalDateTime.now().plusYears(1));
        } else {
            // Limite de sessions actives : 2 par utilisateur
            final int MAX_ACTIVE_SESSIONS = 2;
            long activeSessionsCount = userSessionRepository.countByUserUuidAndIsActiveTrue(user.getUuid());
            
            if (activeSessionsCount >= MAX_ACTIVE_SESSIONS) {
                throw new RuntimeException("SESSION_LIMIT_REACHED");
            }
            
            // Créer une nouvelle session
            session = new UserSession();
            session.setUserUuid(user.getUuid());
            session.setUser(user);
            session.setDeviceId(finalDeviceId);
            String enhancedDeviceName = deviceDetectionService.detectDeviceName(userAgent, deviceName);
            session.setDeviceName(enhancedDeviceName);
            session.setIpAddress(ipAddress);
            session.setUserAgent(userAgent);
            session.setCreatedAt(LocalDateTime.now());
            session.setLastActivity(LocalDateTime.now());
            session.setExpiresAt(LocalDateTime.now().plusYears(1));
            session.setActive(true);
        }

        User admin = null;
        try {
            admin = user.getEntreprise().getAdmin();
        } catch (Exception e) {
            admin = usersRepository.findByUuid(user.getUuid())
                    .map(u -> u.getEntreprise().getAdmin())
                    .orElse(null);
        }
        boolean within24Hours = LocalDateTime.now().isBefore(user.getCreatedAt().plusHours(24));
        

        String accessToken = null;
        
        if (!isExistingSession) {
            Optional<UserSession> lastCheck = userSessionRepository
                    .findByDeviceIdAndUserUuidAndIsActiveTrueWithLock(finalDeviceId, user.getUuid());
            
            if (lastCheck.isPresent()) {
                session = lastCheck.get();
                isExistingSession = true;
            } else {
                try {
                    session = userSessionRepository.save(session);
                    accessToken = generateAccessTokenWithSession(user, admin, within24Hours, session.getId());
                    userSessionRepository.updateSessionToken(session.getId(), accessToken);
                    session.setSessionToken(accessToken);
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // Si une session avec le même deviceId existe déjà (contrainte unique violée)
                    Optional<UserSession> existingSessionOpt = userSessionRepository  
                            .findByDeviceIdAndUserUuidAndIsActiveTrue(finalDeviceId, user.getUuid());
                    
                    if (existingSessionOpt.isPresent()) {
                        session = existingSessionOpt.get();
                        isExistingSession = true;
                    } else {
                        throw new RuntimeException("Erreur lors de la récupération de la session existante après violation de contrainte", e);
                    }
                }
            }
        }
        

        if (isExistingSession && accessToken == null) {
    
            accessToken = generateAccessTokenWithSession(user, admin, within24Hours, session.getId());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusYears(1);
            userSessionRepository.updateSessionActivityAndToken(session.getId(), now, expiresAt, accessToken);
            session.setSessionToken(accessToken);
            session.setLastActivity(now);
            session.setExpiresAt(expiresAt);
        }
        
        if (accessToken == null) {
            throw new RuntimeException("Erreur : le token n'a pas pu être généré");
        }



    Map<String, String> tokens = new HashMap<>();
    tokens.put("accessToken", accessToken);
        tokens.put("deviceId", finalDeviceId);
    return tokens;
}

    public Map<String, String> login(String email, String password) {
        return login(email, password, null, null, null, null);
    }

            public String generateAccessToken(User user, User admin, boolean within24Hours) {
                return generateAccessTokenWithSession(user, admin, within24Hours, null);
            }

            public String generateAccessTokenWithSession(User user, User admin, boolean within24Hours, Long sessionId) {
            long expirationTime = 1000L * 60 * 60 * 24 * 365; 
            Date now = new Date();
            Date expirationDate = new Date(now.getTime() + expirationTime);

            boolean isAdminRole = user.getRole().getName().equals(RoleType.ADMIN)
                    || user.getRole().getName().equals(RoleType.SUPER_ADMIN);

            boolean userActivated = user.isEnabledLien();
            boolean adminActivated = (admin != null) ? admin.isEnabledLien() : true;
            boolean userActivationPossible = isAdminRole ? (user.isActivatedLien() || within24Hours) : true;

            long lastActivityTimestamp = user.getLastActivity() != null 
                    ? user.getLastActivity().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : now.getTime();

                var tokenBuilder = Jwts.builder()
                    .setSubject(user.getUuid())
                    .claim("role", user.getRole().getName())
                    .claim("userActivated", userActivated)
                    .claim("adminActivated", adminActivated)
                    .claim("userActivationPossible", userActivationPossible)
                    .claim("lastActivity", lastActivityTimestamp)
                    .setIssuedAt(now)
                        .setExpiration(expirationDate);

                if (sessionId != null) {
                    tokenBuilder.claim("sessionId", sessionId);
                }

                return tokenBuilder
                    .signWith(SignatureAlgorithm.HS256, jwtConfig.getSecretKey())
                    .compact();
        }
    //Resent Mail
    @Transactional
    public void resendActivationEmail(String email) {
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email."));

        if (user.isActivatedLien()) {
            throw new RuntimeException("Ce compte est déjà activé.");
        }

        try {
            mailService.sendActivationLinkEmail(user.getEmail(), user.getActivationCode(), user.getPersonalCode());
        } catch (MessagingException e) {
            System.err.println("Erreur lors du renvoi de l'email : " + e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email pour le moment.");
        }
    }

    @Transactional
    public void resendEmployeEmail(String email) {
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email."));

        InitialPasswordToken token = initialPasswordTokenRepository
                .findByUser(user)
                .orElseThrow(() -> new RuntimeException("Impossible de renvoyer l'email : le token d'initialisation n'est plus disponible. L'utilisateur doit réinitialiser son mot de passe."));

        if (token.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Le token d'initialisation a expiré. L'utilisateur doit réinitialiser son mot de passe.");
        }

        try {
            mailService.sendEmployeEmail(
                user.getEmail(),
                user.getNomComplet(),
                user.getEntreprise().getNomEntreprise(),
                user.getRole().getName().toString(),
                user.getEmail(),
                token.getGeneratedPassword(),
                user.getPersonalCode()
            );
        } catch (MessagingException e) {
            System.err.println("Erreur lors du renvoi de l'email : " + e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email pour le moment.");
        }
    }



    // Activation du compte via le lien d'activation (email + code PIN)
    @Transactional
    public User activateAccount(String email, String code) {
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.getActivationCode().equals(code)) {
            throw new RuntimeException("Code d'activation invalide.");
        }

        user.setActivatedLien(true);
        user.setEnabledLien(true);
        user = usersRepository.save(user);

        if (user.getRole() != null && user.getRole().getName().equals(RoleType.ADMIN)) {
            Long entrepriseId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;
            if (entrepriseId == null) {
                throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
            }
            List<User> usersToActivate = usersRepository.findByEntrepriseId(entrepriseId);
            usersToActivate.forEach(u -> u.setEnabledLien(true));
            usersRepository.saveAll(usersToActivate);
        }
        
        return user;
    }

    public Map<String, Object> getAccountStatus(String email) {
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Map<String, Object> status = new HashMap<>();
        status.put("email", user.getEmail());
        status.put("activated", user.isActivatedLien());
        status.put("locked", user.isLocked());
        status.put("enabled", user.isEnabledLien());

        LocalDateTime expirationTime = user.getCreatedAt().plusHours(24);
        long minutesRemaining = ChronoUnit.MINUTES.between(LocalDateTime.now(), expirationTime);

        if (minutesRemaining > 0) {
            status.put("timeRemaining", minutesRemaining + " minutes restantes avant expiration.");
        } else {
            status.put("timeRemaining", "Compte expiré.");
        }

        return status;
    }

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
                String token = request.getHeader("Authorization");
                if (token == null || !token.startsWith("Bearer ")) {
                    throw new RuntimeException("Token JWT manquant ou mal formaté");
                }

                User admin = authHelper.getAuthenticatedUserWithFallback(request);

                Entreprise entreprise = admin.getEntreprise();
                if (entreprise == null) {
                    throw new BusinessException("Vous n'avez pas d'entreprise associée.");
                }

                boolean isAdmin = CentralAccess.isAdminOfEntreprise(admin, entreprise.getId());
                boolean hasPermission = admin.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);

                if (!isAdmin && !hasPermission) {
                    throw new RuntimeException("Accès refusé : seuls les administrateurs, managers ou les utilisateurs autorisés peuvent ajouter un employé.");
                }

                Long entrepriseId = admin.getEntreprise() != null ? admin.getEntreprise().getId() : null;
                if (entrepriseId == null) {
                    throw new BusinessException("L'admin n'a pas d'entreprise associée.");
                }

                // Vérification globale de l'email (unique dans toute la base)
                if (usersRepository.findByEmail(userRequest.getEmail()).isPresent()) {
                    throw new BusinessException("Cet email est déjà utilisé par un autre compte.");
                }

                // Vérification globale du téléphone (unique dans toute la base)
                if (usersRepository.findByPhone(userRequest.getPhone()).isPresent()) {
                    throw new BusinessException("Ce numéro de téléphone est déjà utilisé par un autre compte.");
                }

      
                List<Role> existingRoles = roleRepository.findAllByName(userRequest.getRoleType());
                if (existingRoles.isEmpty()) {
                    throw new RuntimeException("Rôle invalide : " + userRequest.getRoleType() + ". Ce rôle n'existe pas dans la base de données.");
                }
                
                Role role;
                
                boolean isRoleWithDefaultPermissions = userRequest.getRoleType() == RoleType.ADMIN 
                        || userRequest.getRoleType() == RoleType.MANAGER;
                
                if (isRoleWithDefaultPermissions) {

                    Role templateRole = existingRoles.get(0);
                    
                    if (templateRole.getPermissions() == null || templateRole.getPermissions().isEmpty()) {
                        throw new RuntimeException("Le rôle " + userRequest.getRoleType() + " doit avoir des permissions par défaut.");
                    }
                    

                    Role reusableRole = null;
                    
                    Set<PermissionType> templatePermissionTypes = templateRole.getPermissions().stream()
                            .map(Permission::getType)
                            .collect(Collectors.toSet());
                    
                    for (Role r : existingRoles) {
                        List<User> usersWithRoleInEntreprise = usersRepository.findByRoleAndEntrepriseId(r, entrepriseId);
                        List<User> allUsersWithRole = usersRepository.findByRole(r);
                        boolean isUsedInSameEntreprise = !usersWithRoleInEntreprise.isEmpty();
                        boolean isNotUsedAnywhere = allUsersWithRole.isEmpty();
                        
                        boolean hasSamePermissions = false;
                        if (r.getPermissions() != null && r.getPermissions().size() == templatePermissionTypes.size()) {
                            Set<PermissionType> rolePermissionTypes = r.getPermissions().stream()
                                    .map(Permission::getType)
                                    .collect(Collectors.toSet());
                            hasSamePermissions = rolePermissionTypes.equals(templatePermissionTypes);
                        }
                        
                    
                        if (hasSamePermissions && (isUsedInSameEntreprise || isNotUsedAnywhere)) {
                            reusableRole = r;
                            break;
                        }
                    }
                    
                    if (reusableRole != null) {
                        role = reusableRole;
                    } else {
                        role = new Role();
                        role.setName(templateRole.getName());
                        role.setPermissions(new ArrayList<>(templateRole.getPermissions()));
                        role = roleRepository.save(role);
                    }
                } else {
                    Role reusableRole = null;
                    
                    for (Role r : existingRoles) {
                        List<User> usersWithRoleInEntreprise = usersRepository.findByRoleAndEntrepriseId(r, entrepriseId);
                        List<User> allUsersWithRole = usersRepository.findByRole(r);
                        boolean hasNoPermissions = r.getPermissions() == null || r.getPermissions().isEmpty();
                        
                        boolean isUsedInSameEntreprise = !usersWithRoleInEntreprise.isEmpty();
                        boolean isNotUsedAnywhere = allUsersWithRole.isEmpty();
                        
                        if (hasNoPermissions && (isUsedInSameEntreprise || isNotUsedAnywhere)) {
                            reusableRole = r;
                            break;
                        }
                    }
                    
                    if (reusableRole != null) {
                        role = reusableRole;
                    } else {
                        role = new Role();
                        role.setName(userRequest.getRoleType());
                        role.setPermissions(new ArrayList<>());
                        role = roleRepository.save(role);
                    }
                }

                String generatedPassword = PasswordGenerator.generatePassword();
                String encodedPassword = passwordEncoder.encode(generatedPassword);

                    String personalCode;
                    boolean isUnique;
                    do {
                        personalCode = String.format("%04d", new Random().nextInt(10000));
                        isUnique = !usersRepository.existsByPersonalCode(personalCode); 
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
                newUser.setUuid(UUID.randomUUID().toString());
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


                User savedUser = usersRepository.save(newUser);

                String initialPasswordTokenValue = UUID.randomUUID().toString();
                InitialPasswordToken initialPasswordToken = 
                    new InitialPasswordToken();
                initialPasswordToken.setToken(initialPasswordTokenValue);
                initialPasswordToken.setUser(savedUser);
                initialPasswordToken.setGeneratedPassword(generatedPassword);
                initialPasswordToken.setExpirationDate(LocalDateTime.now().plusDays(30));
                initialPasswordTokenRepository.save(initialPasswordToken);

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
                } catch (Exception e) {
                    System.err.println(" Erreur lors de l'envoi de l'email à " + savedUser.getEmail() + " : " + e.getMessage());
                }
                return savedUser;
            }

    //Attribution des permissions à un utilisateur
    @Transactional
    public UserDTO assignPermissionsToUser(Long userId, AssignPermissionsRequest request, HttpServletRequest httpRequest) {
        User currentUser = authHelper.getAuthenticatedUserWithFallback(httpRequest);

        User targetUser = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur cible non trouvé"));

        if (currentUser.getEntreprise() == null || targetUser.getEntreprise() == null ||
            !currentUser.getEntreprise().getId().equals(targetUser.getEntreprise().getId())) {
            throw new RuntimeException("Les utilisateurs doivent appartenir à la même entreprise.");
        }

        boolean isTargetAdmin = CentralAccess.isAdminOfEntreprise(targetUser, targetUser.getEntreprise().getId());
        if (isTargetAdmin) {
            throw new RuntimeException("Impossible de modifier les permissions de l'administrateur de l'entreprise.");
        }

        boolean isSelf = currentUser.getId().equals(targetUser.getId());
        boolean isAdmin = CentralAccess.isAdminOfEntreprise(currentUser, currentUser.getEntreprise().getId());
        boolean hasPermission = currentUser.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);

        if (isSelf && !isAdmin) {
            throw new RuntimeException("Vous ne pouvez pas modifier vos propres permissions !");
        }

        if (!isAdmin && !hasPermission) {
            throw new RuntimeException("Accès refusé : seuls les administrateurs ou personnes autorisées peuvent gérer les permissions.");
        }

        if (targetUser.getRole() == null) {
            throw new RuntimeException("L'utilisateur cible n'a pas de rôle attribué.");
        }

        Map<PermissionType, Boolean> permissions = request.getPermissions();
        if (permissions == null || permissions.isEmpty()) {
            throw new RuntimeException("Aucune permission spécifiée.");
        }

        Role targetRole = targetUser.getRole();
        List<User> usersWithSameRole = usersRepository.findByRole(targetRole);

        if (usersWithSameRole.size() > 1) {
            Role clonedRole = new Role();
            clonedRole.setName(targetRole.getName());
            clonedRole.setPermissions(
                    targetRole.getPermissions() != null
                            ? new ArrayList<>(targetRole.getPermissions())
                            : new ArrayList<>()
            );

            clonedRole = roleRepository.save(clonedRole);

            targetUser.setRole(clonedRole);
            usersRepository.save(targetUser);

            targetRole = clonedRole;
        }

        List<Permission> existingPermissions = targetRole.getPermissions();
        if (existingPermissions == null) {
            existingPermissions = new ArrayList<>();
            targetRole.setPermissions(existingPermissions);
        }

        // Gérer les permissions générales
        for (Map.Entry<PermissionType, Boolean> entry : permissions.entrySet()) {
            PermissionType permissionType = entry.getKey();
            Boolean isEnabled = entry.getValue();

            // Traitement spécial pour APPROVISIONNER_STOCK
            if (permissionType == PermissionType.APPROVISIONNER_STOCK) {
                if (Boolean.TRUE.equals(isEnabled)) {
                    // Vérifier que les boutiques sont spécifiées
                    List<Long> boutiqueIds = request.getBoutiqueIdsForStockManagement();
                    if (boutiqueIds == null || boutiqueIds.isEmpty()) {
                        throw new RuntimeException("Pour activer la permission APPROVISIONNER_STOCK, vous devez spécifier au moins une boutique dans 'boutiqueIdsForStockManagement'.");
                    }

                    // Vérifier que toutes les boutiques appartiennent à l'entreprise
                    Long entrepriseId = currentUser.getEntreprise().getId();
                    List<Boutique> boutiques = boutiqueRepository.findAllById(boutiqueIds);
                    if (boutiques.size() != boutiqueIds.size()) {
                        throw new RuntimeException("Certaines boutiques spécifiées n'existent pas.");
                    }
                    for (Boutique boutique : boutiques) {
                        if (!boutique.getEntreprise().getId().equals(entrepriseId)) {
                            throw new RuntimeException("La boutique '" + boutique.getNomBoutique() + "' n'appartient pas à votre entreprise.");
                        }
                    }

                    // Ajouter la permission au rôle si elle n'existe pas déjà
                    Permission stockPermission = permissionRepository.findByType(PermissionType.APPROVISIONNER_STOCK)
                            .orElseThrow(() -> new RuntimeException("Permission APPROVISIONNER_STOCK non trouvée"));
                    if (!existingPermissions.contains(stockPermission)) {
                        existingPermissions.add(stockPermission);
                    }

                    // Assigner la permission de gestion de stock aux boutiques spécifiées
                    for (Boutique boutique : boutiques) {
                        Optional<UserBoutique> userBoutiqueOpt = userBoutiqueRepository
                                .findByUserIdAndBoutiqueId(userId, boutique.getId());
                        
                        UserBoutique userBoutique;
                        if (userBoutiqueOpt.isPresent()) {
                            userBoutique = userBoutiqueOpt.get();
                        } else {
                            // Créer une nouvelle relation UserBoutique
                            userBoutique = new UserBoutique();
                            userBoutique.setUser(targetUser);
                            userBoutique.setBoutique(boutique);
                            userBoutique.setAssignedAt(LocalDateTime.now());
                        }
                        userBoutique.setCanGestionStock(true);
                        userBoutiqueRepository.save(userBoutique);
                    }

                    // Retirer la permission de gestion de stock des boutiques non spécifiées
                    List<UserBoutique> allUserBoutiques = userBoutiqueRepository.findByUserId(userId);
                    for (UserBoutique ub : allUserBoutiques) {
                        if (!boutiqueIds.contains(ub.getBoutique().getId())) {
                            ub.setCanGestionStock(false);
                            userBoutiqueRepository.save(ub);
                        }
                    }
                } else {
                    // Retirer la permission APPROVISIONNER_STOCK du rôle
                    Permission stockPermission = permissionRepository.findByType(PermissionType.APPROVISIONNER_STOCK)
                            .orElse(null);
                    if (stockPermission != null) {
                        existingPermissions.remove(stockPermission);
                    }

                    // Retirer la permission de gestion de stock de toutes les boutiques
                    List<UserBoutique> allUserBoutiques = userBoutiqueRepository.findByUserId(userId);
                    for (UserBoutique ub : allUserBoutiques) {
                        ub.setCanGestionStock(false);
                        userBoutiqueRepository.save(ub);
                    }
                }
            } else if (permissionType == PermissionType.GERER_PRODUITS) {
                // Traitement spécial pour GERER_PRODUITS
                if (Boolean.TRUE.equals(isEnabled)) {
                    // Vérifier que les boutiques sont spécifiées
                    List<Long> boutiqueIds = request.getBoutiqueIdsForProductManagement();
                    if (boutiqueIds == null || boutiqueIds.isEmpty()) {
                        throw new RuntimeException("Pour activer la permission GERER_PRODUITS, vous devez spécifier au moins une boutique dans 'boutiqueIdsForProductManagement'.");
                    }

                    // Vérifier que toutes les boutiques appartiennent à l'entreprise
                    Long entrepriseId = currentUser.getEntreprise().getId();
                    List<Boutique> boutiques = boutiqueRepository.findAllById(boutiqueIds);
                    if (boutiques.size() != boutiqueIds.size()) {
                        throw new RuntimeException("Certaines boutiques spécifiées n'existent pas.");
                    }
                    for (Boutique boutique : boutiques) {
                        if (!boutique.getEntreprise().getId().equals(entrepriseId)) {
                            throw new RuntimeException("La boutique '" + boutique.getNomBoutique() + "' n'appartient pas à votre entreprise.");
                        }
                    }

                    // Ajouter la permission au rôle si elle n'existe pas déjà
                    Permission productPermission = permissionRepository.findByType(PermissionType.GERER_PRODUITS)
                            .orElseThrow(() -> new RuntimeException("Permission GERER_PRODUITS non trouvée"));
                    if (!existingPermissions.contains(productPermission)) {
                        existingPermissions.add(productPermission);
                    }

                    // Assigner la permission de gestion de produits aux boutiques spécifiées
                    for (Boutique boutique : boutiques) {
                        Optional<UserBoutique> userBoutiqueOpt = userBoutiqueRepository
                                .findByUserIdAndBoutiqueId(userId, boutique.getId());
                        
                        UserBoutique userBoutique;
                        if (userBoutiqueOpt.isPresent()) {
                            userBoutique = userBoutiqueOpt.get();
                        } else {
                            // Créer une nouvelle relation UserBoutique
                            userBoutique = new UserBoutique();
                            userBoutique.setUser(targetUser);
                            userBoutique.setBoutique(boutique);
                            userBoutique.setAssignedAt(LocalDateTime.now());
                        }
                        userBoutique.setCanGererProduits(true);
                        userBoutiqueRepository.save(userBoutique);
                    }

                    // Retirer la permission de gestion de produits des boutiques non spécifiées
                    List<UserBoutique> allUserBoutiques = userBoutiqueRepository.findByUserId(userId);
                    for (UserBoutique ub : allUserBoutiques) {
                        if (!boutiqueIds.contains(ub.getBoutique().getId())) {
                            ub.setCanGererProduits(false);
                            userBoutiqueRepository.save(ub);
                        }
                    }
                } else {
                    // Retirer la permission GERER_PRODUITS du rôle
                    Permission productPermission = permissionRepository.findByType(PermissionType.GERER_PRODUITS)
                            .orElse(null);
                    if (productPermission != null) {
                        existingPermissions.remove(productPermission);
                    }

                    // Retirer la permission de gestion de produits de toutes les boutiques
                    List<UserBoutique> allUserBoutiques = userBoutiqueRepository.findByUserId(userId);
                    for (UserBoutique ub : allUserBoutiques) {
                        ub.setCanGererProduits(false);
                        userBoutiqueRepository.save(ub);
                    }
                }
            } else {
                // Traitement normal pour les autres permissions
                Permission permission = permissionRepository.findByType(permissionType)
                        .orElseThrow(() -> new RuntimeException("Permission non trouvée : " + permissionType));

                if (Boolean.TRUE.equals(isEnabled)) {
                    if (!existingPermissions.contains(permission)) {
                        existingPermissions.add(permission);
                    }
                } else {
                    existingPermissions.remove(permission);
                }
            }
        }

        roleRepository.save(targetRole);

        // Recharger l'utilisateur pour avoir les UserBoutiques à jour
        targetUser = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur cible non trouvé"));

        List<PermissionDTO> permissionsDTO = targetUser.getRole().getPermissions().stream()
                .map(permission -> new PermissionDTO(permission.getId(), permission.getType().toString()))
                .collect(Collectors.toList());

        RoleDTO roleDTO = new RoleDTO(targetUser.getRole().getId(), targetUser.getRole().getName().toString(), permissionsDTO);

        // Convertir les UserBoutiques en DTO
        List<UserBoutiqueDTO> userBoutiquesDTO = convertUserBoutiquesToDTO(targetUser);

        return new UserDTO(
            targetUser.getId(),
            targetUser.getUuid(),
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
            userBoutiquesDTO
        );
    }

    /**
     * Convertit une liste de UserBoutique en liste de UserBoutiqueDTO.
     * Pour ADMIN et MANAGER, retourne toutes les boutiques de l'entreprise avec isGestionnaireStock = true.
     * Pour les autres rôles, retourne uniquement les UserBoutiques de l'utilisateur.
     */
    private List<UserBoutiqueDTO> convertUserBoutiquesToDTO(User user) {
        if (user == null || user.getRole() == null || user.getEntreprise() == null) {
            return new ArrayList<>();
        }

        RoleType roleType = user.getRole().getName();
        
        // ADMIN et MANAGER: toutes les boutiques de l'entreprise
        if (roleType == RoleType.ADMIN || roleType == RoleType.MANAGER) {
            List<Boutique> boutiques = boutiqueRepository.findByEntrepriseId(user.getEntreprise().getId());
            boolean hasStockPermission = user.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK);
            boolean hasProductPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
            
            return boutiques.stream()
                    .map(boutique -> new UserBoutiqueDTO(
                            boutique.getId(),
                            boutique.getNomBoutique(),
                            boutique.isActif(),
                            boutique.getTypeBoutique(),
                            hasStockPermission,
                            hasProductPermission
                    ))
                    .collect(Collectors.toList());
        }

        // Pour les autres rôles, retourner uniquement les UserBoutiques de l'utilisateur
        return user.getUserBoutiques().stream()
                .map(ub -> {
                    Boutique boutique = ub.getBoutique();
                    return new UserBoutiqueDTO(
                            boutique.getId(),
                            boutique.getNomBoutique(),
                            boutique.isActif(),
                            boutique.getTypeBoutique(),
                            ub.isCanGestionStock(),
                            ub.isCanGererProduits()
                    );
                })
                .collect(Collectors.toList());
    }

    //Suprim UserToEntreprise 
    @Transactional
    public void deleteUserFromEntreprise(HttpServletRequest request, Long userId) {
    User admin = authHelper.getAuthenticatedUserWithFallback(request);

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

    if (admin.getId().equals(userId)) {
        throw new RuntimeException("Vous ne pouvez pas supprimer votre propre compte.");
    }

    if (role == RoleType.MANAGER && userToDelete.getRole().getName() == RoleType.ADMIN) {
        throw new RuntimeException("Un Manager ne peut pas supprimer un Admin.");
    }

    if (!userToDelete.getEntreprise().equals(admin.getEntreprise())) {
        throw new RuntimeException("Vous ne pouvez supprimer que les utilisateurs de votre entreprise.");
    }

    List<FactureProForma> facturesLiees = factureProformaRepository.findByUtilisateurCreateurIdAndEntrepriseId(
            userId, admin.getEntreprise().getId());
    if (!facturesLiees.isEmpty()) {
        throw new RuntimeException("Impossible de supprimer cet utilisateur : il est lié à des factures.");
    }

    if (userToDelete.getQrCodeUrl() != null) {
        try {
            imageStorageService.deleteQrCodeImage(userToDelete.getQrCodeUrl());
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression du QR Code : " + e.getMessage());
            throw new RuntimeException("Impossible de supprimer le QR Code de l'utilisateur.", e);
        }
    }
   

    initialPasswordTokenRepository.deleteByUser(userToDelete);

    usersRepository.delete(userToDelete);
}


    // Pour la modification de utilisateur
   @Transactional
    public User updateUser(Long userId, UpdateUserRequest request, MultipartFile imageUserFile, Boolean deletePhoto) {
    User user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

    boolean isSensitiveChange =
            (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) ||
            (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) ||
            (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) ||
            (request.getNomComplet() != null && !request.getNomComplet().equals(user.getNomComplet()));

    if (isSensitiveChange) {
        if (request.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect. Modification refusée.");
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isEmpty()) {
            if (request.getNewPassword().length() < 8) {
                throw new RuntimeException("Le nouveau mot de passe doit contenir au moins 8 caractères.");
            }
            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                throw new RuntimeException("Le nouveau mot de passe ne peut pas être identique à l'ancien.");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            initialPasswordTokenRepository.deleteByUser(user);
        }

        Long entrepriseId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;
        if (entrepriseId == null) {
            throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
        }

        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            Optional<User> existingUserWithPhone = usersRepository.findByPhoneAndEntrepriseId(request.getPhone(), entrepriseId);
            if (existingUserWithPhone.isPresent() && !existingUserWithPhone.get().getId().equals(userId)) {
                throw new RuntimeException("Ce numéro de téléphone est déjà utilisé par un autre utilisateur dans votre entreprise.");
            }
            user.setPhone(request.getPhone());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            Optional<User> existingUserWithEmail = usersRepository.findByEmailAndEntrepriseId(request.getEmail(), entrepriseId);
            if (existingUserWithEmail.isPresent() && !existingUserWithEmail.get().getId().equals(userId)) {
                throw new RuntimeException("Cet email est déjà utilisé par un autre utilisateur dans votre entreprise.");
            }
            user.setEmail(request.getEmail());
        }

       if (request.getNomComplet() != null) {
           user.setNomComplet(request.getNomComplet());
       }
    }

    if (imageUserFile != null && !imageUserFile.isEmpty()) {
        String oldImagePath = user.getPhoto();
        if (oldImagePath != null && !oldImagePath.isBlank()) {
            Path oldPath = Paths.get("src/main/resources/static" + oldImagePath);
            try {
                Files.deleteIfExists(oldPath);
                System.out.println(" Ancien photo profile supprimé : " + oldImagePath);
            } catch (IOException e) {
                System.out.println(" Impossible de supprimer l'ancien photo profile : " + e.getMessage());
            }
        }

        String newImageUrl = imageStorageService.saveUserImage(imageUserFile);
        user.setPhoto(newImageUrl);
        System.out.println(" Nouveau logo enregistré : " + newImageUrl);
    }

    System.out.println(" DTO reçu dans le controller : " + request);

        if (Boolean.TRUE.equals(deletePhoto)) {
        String oldImagePath = user.getPhoto();
        if (oldImagePath != null && !oldImagePath.isBlank()) {
            Path oldPath = Paths.get("src/main/resources/static" + oldImagePath);
            try {
                Files.deleteIfExists(oldPath);
                System.out.println(" Ancienne photo supprimée : " + oldImagePath);
            } catch (IOException e) {
                System.out.println(" Erreur suppression photo : " + e.getMessage());
            }
            user.setPhoto(null);
        }
    }
    usersRepository.save(user);
    return user;
}

    //Get user info
    public UserRequest getInfo(Long userId) {
        
        User user = usersRepository.findByIdWithEntrepriseAndRole(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    Entreprise entreprise = user.getEntreprise();

    List<BoutiqueResponse> boutiqueResponses;
    String roleType = user.getRole().getName().name();
    RoleType roleTypeEnum = user.getRole().getName();

    if ("VENDEUR".equals(roleType)) {
        // Boutiques assignées au vendeur
        boutiqueResponses = user.getUserBoutiques()
                .stream()
                .map(ub -> {
                    Boutique b = ub.getBoutique();
                    BoutiqueResponse response = new BoutiqueResponse(
                            b.getId(),
                            b.getNomBoutique(),
                            b.getAdresse(),
                            b.getTelephone(),
                            b.getEmail(),
                            b.getCreatedAt(),
                            b.isActif(),
                            b.getTypeBoutique(),
                            ub.isCanGestionStock(),
                            ub.isCanGererProduits()
                    );
                    return response;
                })
                .collect(Collectors.toList());
    } else {
        // ADMIN ou MANAGER : toutes les boutiques de l'entreprise
        boolean isAdminOrManager = (roleTypeEnum == RoleType.ADMIN || roleTypeEnum == RoleType.MANAGER);
        boolean hasStockPermission = user.getRole().hasPermission(PermissionType.APPROVISIONNER_STOCK);
        boolean hasProductPermission = user.getRole().hasPermission(PermissionType.GERER_PRODUITS);
        
        boutiqueResponses = entreprise.getBoutiques()
                .stream()
                .map(b -> {
                    Boolean isGestionnaire = null;
                    Boolean isGererProduits = null;
                    
                    if (isAdminOrManager) {
                        isGestionnaire = hasStockPermission;
                        isGererProduits = hasProductPermission;
                    } else {
                        // Pour les autres rôles, vérifier dans UserBoutiques
                        if (hasStockPermission) {
                            Optional<UserBoutique> userBoutiqueOpt = user.getUserBoutiques().stream()
                                    .filter(ub -> ub.getBoutique().getId().equals(b.getId()))
                                    .findFirst();
                            if (userBoutiqueOpt.isPresent()) {
                                isGestionnaire = userBoutiqueOpt.get().isCanGestionStock();
                            } else {
                                isGestionnaire = false;
                            }
                        } else {
                            isGestionnaire = false;
                        }
                        
                        if (hasProductPermission) {
                            Optional<UserBoutique> userBoutiqueOpt = user.getUserBoutiques().stream()
                                    .filter(ub -> ub.getBoutique().getId().equals(b.getId()))
                                    .findFirst();
                            if (userBoutiqueOpt.isPresent()) {
                                isGererProduits = userBoutiqueOpt.get().isCanGererProduits();
                            } else {
                                isGererProduits = false;
                            }
                        } else {
                            isGererProduits = false;
                        }
                    }
                    
                    return new BoutiqueResponse(
                            b.getId(),
                            b.getNomBoutique(),
                            b.getAdresse(),
                            b.getTelephone(),
                            b.getEmail(),
                            b.getCreatedAt(),
                            b.isActif(),
                            b.getTypeBoutique(),
                            isGestionnaire,
                            isGererProduits
                    );
                })
                .collect(Collectors.toList());
    }

    // Permissions
    List<String> permissions = user.getRole().getPermissions()
            .stream()
            .map(p -> p.getType().name())
            .collect(Collectors.toList());

    return new UserRequest(user, entreprise, boutiqueResponses, permissions);
}

   

// Pour la récupération de tous les utilisateurs d'une entreprise
 public List<UserDTO> getAllUsersOfEntreprise(HttpServletRequest request) {
    String token = request.getHeader("Authorization");
    if (token == null || !token.startsWith("Bearer ")) {
        throw new RuntimeException("Token JWT manquant ou mal formaté");
    }

    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur.");
    }

    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);
    boolean isComptable = user.getRole().hasPermission(PermissionType.VOIR_FLUX_COMPTABLE);
   

    

    if (!isAdminOrManager && !hasPermission && isComptable) {
        throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires.");
    }

    List<User> users = usersRepository.findByEntrepriseId(entreprise.getId());

    return users.stream().map(userEntity -> {
        List<PermissionDTO> permissionsDTO = userEntity.getRole().getPermissions().stream()
            .map(permission -> new PermissionDTO(permission.getId(), permission.getType().toString()))
            .collect(Collectors.toList());

        RoleDTO roleDTO = new RoleDTO(userEntity.getRole().getId(), userEntity.getRole().getName().toString(), permissionsDTO);

        // Convertir les UserBoutiques en DTO
        List<UserBoutiqueDTO> userBoutiquesDTO = convertUserBoutiquesToDTO(userEntity);

        return new UserDTO(
            userEntity.getId(),
            userEntity.getUuid(),
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
            userBoutiquesDTO
        );
    }).collect(Collectors.toList());
}


    //Get user by id
  public UserDTO getUserById(Long userId, HttpServletRequest request) {

    User connectedUser = authHelper.getAuthenticatedUserWithFallback(request);

    User targetUser = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Utilisateur demandé introuvable"));

    boolean hasGestionUtilisateurPermission = targetUser.getRole() != null &&
            targetUser.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);

    RoleType role = connectedUser.getRole().getName();
    boolean isAdminOrManager = (role == RoleType.ADMIN || role == RoleType.MANAGER)
            && connectedUser.getEntreprise() != null
            && targetUser.getEntreprise() != null
            && connectedUser.getEntreprise().getId().equals(targetUser.getEntreprise().getId());

    boolean isSelf = connectedUser.getId().equals(userId);

    if (!isAdminOrManager && !isSelf && !hasGestionUtilisateurPermission) {
        throw new RuntimeException("Accès interdit : vous ne pouvez consulter que vos propres informations !");
    }

    List<PermissionDTO> permissionsDTO = targetUser.getRole().getPermissions().stream()
            .map(permission -> new PermissionDTO(permission.getId(), permission.getType().toString()))
            .collect(Collectors.toList());

    RoleDTO roleDTO = new RoleDTO(targetUser.getRole().getId(), targetUser.getRole().getName().toString(), permissionsDTO);

        // Convertir les UserBoutiques en DTO
        List<UserBoutiqueDTO> userBoutiquesDTO = convertUserBoutiquesToDTO(targetUser);

        UserDTO userDTO = new UserDTO(
        targetUser.getId(),
        targetUser.getUuid(),
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
        userBoutiquesDTO
    );

    return userDTO;
}

    // Méthode pour suspendre ou réactiver un utilisateur
    @Transactional
    public void suspendUser(HttpServletRequest request, Long userId, boolean suspend) {
        User currentUser = authHelper.getAuthenticatedUserWithFallback(request);

        User targetUser = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur cible non trouvé"));

        if (currentUser.getEntreprise() == null || targetUser.getEntreprise() == null ||
            !currentUser.getEntreprise().getId().equals(targetUser.getEntreprise().getId())) {
            throw new RuntimeException("Les utilisateurs doivent appartenir à la même entreprise.");
        }

        
        boolean isSelf = currentUser.getId().equals(targetUser.getId());
        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(currentUser, currentUser.getEntreprise().getId());
        boolean hasPermission = currentUser.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);

        if (isSelf && !isAdminOrManager) {
            throw new RuntimeException("Vous ne pouvez pas vous suspendre vous-même !");
        }

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : seuls les administrateurs ou personnes autorisées peuvent suspendre/réactiver des utilisateurs.");
        }

        boolean isTargetAdmin = CentralAccess.isAdminOfEntreprise(targetUser, targetUser.getEntreprise().getId());
        if (isTargetAdmin) {
            throw new RuntimeException("Impossible de suspendre l'administrateur de l'entreprise.");
        }

        targetUser.setEnabledLien(!suspend);
        usersRepository.save(targetUser);
    }

    // Methode pour nombre de utilisateurs dans l'entreprise
    public int countUsersInEntreprise(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        token = token.replace("Bearer ", "");
        User user = authHelper.getAuthenticatedUserWithFallback(request);

        Entreprise entreprise = user.getEntreprise();
        if (entreprise == null) {
            throw new RuntimeException("Aucune entreprise associée à cet utilisateur.");
        }

        boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
        boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);

        if (!isAdminOrManager && !hasPermission) {
            throw new RuntimeException("Accès refusé : vous n'avez pas les droits nécessaires.");
        }

      return (int) usersRepository.countByEntrepriseIdExcludingRole(entreprise.getId(), RoleType.ADMIN);


    }
   

    //Methode qui recupere linformation de lentrprise de user connecter
    public EntrepriseDTO getEntrepriseOfConnectedUser(HttpServletRequest request) {
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Entreprise associée à l'utilisateur non trouvée");
    }
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
    // Téléphone et pays : priorité à l'entreprise, sinon ceux de l'admin (fournis à l'inscription)
    String telephone = (entreprise.getTelephone() != null && !entreprise.getTelephone().isBlank())
            ? entreprise.getTelephone() : user.getPhone();
    String paysEntreprise = (entreprise.getPays() != null && !entreprise.getPays().isBlank())
            ? entreprise.getPays() : user.getPays();
    // Préfixer l'indicatif pays (+223, etc.) si le numéro ne commence pas déjà par +
    if (telephone != null && !telephone.isBlank() && !telephone.trim().startsWith("+")) {
        String indicatif = Utilitaire.getIndicatifPays(paysEntreprise);
        if (indicatif != null) telephone = indicatif + " " + telephone.trim();
    }
    dto.setTelephone(telephone);
    dto.setPays(paysEntreprise);
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

    @Transactional(readOnly = true)
    public UserOptimalDTO getDashboardData(HttpServletRequest request) {
        User currentUser = authHelper.getAuthenticatedUserWithFallback(request);

        if (currentUser.getEntreprise() == null) {
            throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
        }

        Long entrepriseId = currentUser.getEntreprise().getId();

        UserOptimalDTO.UserInfoDTO userInfo = new UserOptimalDTO.UserInfoDTO();
        userInfo.setId(currentUser.getId());
        userInfo.setNomComplet(currentUser.getNomComplet());
        userInfo.setEmail(currentUser.getEmail());
        userInfo.setRoleType(currentUser.getRole() != null ? currentUser.getRole().getName().toString() : null);
        userInfo.setUuid(currentUser.getUuid());
        userInfo.setPays(currentUser.getPays());
        userInfo.setPhone(currentUser.getPhone());

        List<UserOptimalDTO.RoleDTO> roles = roleRepository.findAll().stream()
                .map(role -> {
                    UserOptimalDTO.RoleDTO roleDTO = new UserOptimalDTO.RoleDTO();
                    roleDTO.setId(role.getId());
                    roleDTO.setName(role.getName().toString());
                    roleDTO.setDescription(getRoleDescription(role.getName()));
                    return roleDTO;
                })
                .collect(Collectors.toList());

        List<UserOptimalDTO.BoutiqueDTO> boutiques = boutiqueRepository.findByEntrepriseId(entrepriseId).stream()
                .map(boutique -> {
                    UserOptimalDTO.BoutiqueDTO boutiqueDTO = new UserOptimalDTO.BoutiqueDTO();
                    boutiqueDTO.setId(boutique.getId());
                    boutiqueDTO.setNom(boutique.getNomBoutique());
                    boutiqueDTO.setAdresse(boutique.getAdresse());
                    boutiqueDTO.setTelephone(boutique.getTelephone());
                    return boutiqueDTO;
                })
                .collect(Collectors.toList());

        List<UserOptimalDTO.UserDTO> users = usersRepository.findByEntrepriseId(entrepriseId).stream()
                .map(user -> {
                    UserOptimalDTO.UserDTO userDTO = new UserOptimalDTO.UserDTO();
                    userDTO.setId(user.getId());
                    userDTO.setNomComplet(user.getNomComplet());
                    userDTO.setEmail(user.getEmail());
                    userDTO.setRoleType(user.getRole() != null ? user.getRole().getName().toString() : null);
                    userDTO.setUuid(user.getUuid());
                    userDTO.setPays(user.getPays());
                    userDTO.setPhone(user.getPhone());

                    if (user.getRole() != null) {
                        UserOptimalDTO.RoleDTO roleDTO = new UserOptimalDTO.RoleDTO();
                        roleDTO.setId(user.getRole().getId());
                        roleDTO.setName(user.getRole().getName().toString());
                        roleDTO.setDescription(getRoleDescription(user.getRole().getName()));
                        userDTO.setRole(roleDTO);

                        List<UserOptimalDTO.PermissionDTO> permissions = user.getRole().getPermissions().stream()
                                .map(permission -> {
                                    UserOptimalDTO.PermissionDTO permissionDTO = new UserOptimalDTO.PermissionDTO();
                                    permissionDTO.setId(permission.getId());
                                    permissionDTO.setType(permission.getType().toString());
                                    permissionDTO.setDescription(getPermissionDescription(permission.getType()));
                                    return permissionDTO;
                                })
                                .collect(Collectors.toList());
                        userDTO.setPermissions(permissions);
                    } else {
                        userDTO.setPermissions(new ArrayList<>());
                    }

                    return userDTO;
                })
                .collect(Collectors.toList());

        String currentUserRole = currentUser.getRole() != null ? currentUser.getRole().getName().toString() : null;

        UserOptimalDTO dashboard = new UserOptimalDTO();
        dashboard.setUserInfo(userInfo);
        dashboard.setRoles(roles);
        dashboard.setBoutiques(boutiques);
        dashboard.setUsers(users);
        dashboard.setCurrentUserRole(currentUserRole);

        return dashboard;
    }

    private String getRoleDescription(RoleType roleType) {
        return ROLE_DESCRIPTIONS.getOrDefault(roleType, "Rôle non défini");
    }

    private String getPermissionDescription(PermissionType permissionType) {
        return PERMISSION_DESCRIPTIONS.getOrDefault(permissionType, "Permission non définie");
    }

    /**
     * Récupère toutes les sessions actives de l'utilisateur connecté
     */
    // Méthode pour récupérer les sessions actives par userUuid (sans authentification)
    public List<com.xpertcash.DTOs.UserSessionDTO> getActiveSessionsByUserUuid(String userUuid) {
        List<UserSession> sessions = userSessionRepository.findByUserUuidAndIsActiveTrue(userUuid);
        return sessions.stream()
                .map(session -> {
                    com.xpertcash.DTOs.UserSessionDTO dto = new com.xpertcash.DTOs.UserSessionDTO();
                    dto.setId(session.getId());
                    dto.setDeviceId(session.getDeviceId());
                    dto.setDeviceName(session.getDeviceName());
                    dto.setIpAddress(session.getIpAddress());
                    dto.setUserAgent(session.getUserAgent());
                    dto.setCreatedAt(session.getCreatedAt());
                    dto.setLastActivity(session.getLastActivity());
                    dto.setExpiresAt(session.getExpiresAt());
                    dto.setActive(session.isActive());
                    dto.setCurrentSession(false);
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    public User findUserByEmail(String email) {
        return usersRepository.findByEmail(email).orElse(null);
    }
    
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    // Méthode pour fermer une session spécifique avant login (quand limite atteinte)
    @Transactional
    public void closeSessionBeforeLogin(String email, String password, Long sessionId) {
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }
        
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session introuvable"));
        
        if (!session.getUserUuid().equals(user.getUuid())) {
            throw new RuntimeException("Cette session ne vous appartient pas");
        }
        
        userSessionRepository.delete(session);
    }
    
    public List<com.xpertcash.DTOs.UserSessionDTO> getActiveSessions(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        List<UserSession> sessions = userSessionRepository
                .findByUserUuidAndIsActiveTrue(user.getUuid());
        
        String authHeader = request.getHeader("Authorization");
        Long currentSessionId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            Claims claims = jwtUtil.extractAllClaimsSafe(token);
            if (claims != null) {
                Object sessionIdClaim = claims.get("sessionId");
                if (sessionIdClaim != null) {
                    currentSessionId = ((Number) sessionIdClaim).longValue();
                }
            }
        }
        
        final Long finalCurrentSessionId = currentSessionId;
        
        return sessions.stream()
                .map(session -> {
                    com.xpertcash.DTOs.UserSessionDTO dto = new com.xpertcash.DTOs.UserSessionDTO();
                    dto.setId(session.getId());
                    dto.setDeviceId(session.getDeviceId());
                    dto.setDeviceName(session.getDeviceName());
                    dto.setIpAddress(session.getIpAddress());
                    dto.setUserAgent(session.getUserAgent());
                    dto.setCreatedAt(session.getCreatedAt());
                    dto.setLastActivity(session.getLastActivity());
                    dto.setExpiresAt(session.getExpiresAt());
                    dto.setActive(session.isActive());
                    dto.setCurrentSession(finalCurrentSessionId != null && 
                                         finalCurrentSessionId.equals(session.getId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Révoque une session spécifique
     */
    @Transactional
    public void revokeSession(Long sessionId, HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        // Vérifier que la session appartient à l'utilisateur
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session introuvable"));
        
        if (!session.getUserUuid().equals(user.getUuid())) {
            throw new RuntimeException("Vous ne pouvez révoquer que vos propres sessions.");
        }
        
        // Révoquer la session
        session.setActive(false);
        userSessionRepository.save(session);
    }

    /**
     * Révoque toutes les sessions sauf la session courante
     */
    @Transactional
    public void revokeOtherSessions(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }
        
        String token = authHeader.substring(7);
        Claims claims = jwtUtil.extractAllClaimsSafe(token);
        if (claims == null) {
            throw new RuntimeException("Token invalide ou expiré");
        }
        
        Object sessionIdClaim = claims.get("sessionId");
        if (sessionIdClaim == null) {
            throw new RuntimeException("Session ID non trouvé dans le token");
        }
        
        Long currentSessionId = ((Number) sessionIdClaim).longValue();
        
        userSessionRepository.revokeAllSessionsExcept(user.getUuid(), currentSessionId);
    }

    // Récupérer les utilisateurs ayant la permission de vendre des produits
    public List<VendeurDTO> getVendeurs(HttpServletRequest request) {
        User currentUser = authHelper.getAuthenticatedUserWithFallback(request);
        
        Entreprise entreprise = currentUser.getEntreprise();
        if (entreprise == null) {
            throw new BusinessException("Vous n'avez pas d'entreprise associée.");
        }

        List<User> vendeurs = usersRepository.findByEntrepriseIdAndPermission(
                entreprise.getId(), 
                PermissionType.VENDRE_PRODUITS
        );

        return vendeurs.stream()
                .map(this::convertToVendeurDTO)
                .collect(Collectors.toList());
    }

    // Convertir un User en VendeurDTO simplifié
    private VendeurDTO convertToVendeurDTO(User user) {
        List<VendeurDTO.BoutiqueSimpleDTO> boutiques = new ArrayList<>();
        
        RoleType roleType = user.getRole().getName();
        
        // ADMIN et MANAGER: accès à toutes les boutiques de l'entreprise
        if (roleType == RoleType.ADMIN || roleType == RoleType.MANAGER) {
            List<Boutique> allBoutiques = boutiqueRepository.findByEntrepriseId(user.getEntreprise().getId());
            boutiques = allBoutiques.stream()
                    .map(b -> new VendeurDTO.BoutiqueSimpleDTO(b.getId(), b.getNomBoutique()))
                    .collect(Collectors.toList());
        } else {
            // Autres rôles: uniquement leurs boutiques assignées
            List<UserBoutique> userBoutiques = userBoutiqueRepository.findByUserId(user.getId());
            boutiques = userBoutiques.stream()
                    .map(ub -> new VendeurDTO.BoutiqueSimpleDTO(
                            ub.getBoutique().getId(), 
                            ub.getBoutique().getNomBoutique()))
                    .collect(Collectors.toList());
        }

        return new VendeurDTO(
                user.getId(),
                user.getNomComplet(),
                "VENDRE_PRODUITS",
                boutiques
        );
    }

}
