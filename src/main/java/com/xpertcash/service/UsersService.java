package com.xpertcash.service;

import com.xpertcash.DTOs.EntrepriseDTO;
import com.xpertcash.DTOs.UpdateUserRequest;
import com.xpertcash.DTOs.Boutique.BoutiqueResponse;
import com.xpertcash.DTOs.USER.PermissionDTO;
import com.xpertcash.DTOs.USER.RegisterResponse;
import com.xpertcash.DTOs.USER.RoleDTO;
import com.xpertcash.DTOs.USER.UserDTO;
import com.xpertcash.DTOs.USER.UserRequest;
import com.xpertcash.DTOs.UserOptimalDTO;
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

    // Maps statiques pour les descriptions (O(1) lookup)
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
    private ImageStorageService imageStorageService;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private FactureProformaRepository factureProformaRepository;

    @Autowired
    private com.xpertcash.repository.UserSessionRepository userSessionRepository;

    @Autowired
    private DeviceDetectionService deviceDetectionService;

    @Autowired
    private com.xpertcash.repository.PASSWORD.InitialPasswordTokenRepository initialPasswordTokenRepository;

    @Autowired
    public UsersService(UsersRepository usersRepository, JwtConfig jwtConfig, BCryptPasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.jwtConfig = jwtConfig;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Logout : supprime immédiatement la session courante de la base de données
     * Permet à l'utilisateur de rester connecté sur d'autres appareils
     */
    @Transactional
    public void logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Token JWT manquant ou mal formaté");
        }

        String token = authHeader.substring(7);

        // Extraire l'UUID de l'utilisateur et le sessionId depuis le token
        Claims claims = jwtUtil.extractAllClaimsSafe(token);
        if (claims == null) {
            throw new RuntimeException("Token invalide ou expiré");
        }

        String userUuid = claims.getSubject();
        if (userUuid == null) {
            throw new RuntimeException("UUID utilisateur non trouvé dans le token");
        }

        // Récupérer la session par sessionId dans le token (méthode principale)
        com.xpertcash.entity.UserSession session = null;
        Object sessionIdClaim = claims.get("sessionId");
        
        if (sessionIdClaim != null) {
            // Méthode principale : chercher par sessionId (le plus fiable)
            Long sessionId = ((Number) sessionIdClaim).longValue();
            session = userSessionRepository.findById(sessionId).orElse(null);
        }
        
        // Fallback : chercher par token si sessionId n'est pas présent (anciens tokens)
        if (session == null) {
            session = userSessionRepository.findBySessionToken(token).orElse(null);
        }

        if (session != null) {
            // Supprimer immédiatement la session de la base de données
            userSessionRepository.delete(session);
        } else {
            // Si la session n'existe pas, c'est probablement un ancien token sans sessionId
            // On ne fait RIEN pour éviter d'invalider toutes les sessions par erreur
            // L'utilisateur devra simplement se reconnecter
            // Ne pas appeler revokeAllSessions() car cela invaliderait toutes les sessions actives
        }
    }



   @Transactional(noRollbackFor = MessagingException.class)
    public RegisterResponse registerUsers(String nomComplet, String email, String password, String phone, String pays, String nomEntreprise, String nomBoutique) {
        RegisterResponse response = new RegisterResponse();

        // Vérification des données déjà existantes
        if (usersRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé.");
        }
        if (usersRepository.findByPhone(phone).isPresent()) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé.");
        }
        if (entrepriseRepository.findByNomEntreprise(nomEntreprise).isPresent()) {
            throw new RuntimeException("Le nom de l'entreprise est déjà utilisé.");
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
       
    
        // Attribution du rôle ADMIN à l'utilisateur
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
    // Supporte maintenant les sessions multiples par appareil
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED, 
                   rollbackFor = Exception.class)
    public Map<String, String> login(String email, String password, String deviceId, String deviceName, String ipAddress, String userAgent) {
    User user = usersRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

    // Vérifier si l'entreprise de l'utilisateur est désactivée
    Entreprise entreprise = user.getEntreprise();
    // On considère qu'une valeur null pour "active" signifie "active" (compatibilité anciennes données)
    if (entreprise != null && Boolean.FALSE.equals(entreprise.getActive())) {
        throw new RuntimeException("Cette entreprise est désactivée. La connexion est impossible.");
    }

    if (!passwordEncoder.matches(password, user.getPassword())) {
        throw new RuntimeException("Email ou mot de passe incorrect");
    }

    if (user.isLocked()) {
        throw new RuntimeException("Votre compte est verrouillé pour inactivité.");
    }

    // NE PAS charger l'entreprise/admin ici pour éviter les deadlocks
    // On le chargera plus tard, après les opérations sur les sessions
    // User admin = user.getEntreprise().getAdmin(); // DÉPLACÉ PLUS BAS
    // boolean within24Hours = LocalDateTime.now().isBefore(user.getCreatedAt().plusHours(24)); // DÉPLACÉ PLUS BAS

        // Générer deviceId si non fourni (avant les opérations de session pour éviter les deadlocks)
        final String finalDeviceId;
        if (deviceId == null || deviceId.trim().isEmpty()) {
            finalDeviceId = UUID.randomUUID().toString();
        } else {
            finalDeviceId = deviceId;
        }

        // Vérifier si une session existe déjà pour ce deviceId (sans verrou pour optimiser)
        // Le verrou sera utilisé uniquement lors de la création pour éviter les race conditions
        com.xpertcash.entity.UserSession existingSession = userSessionRepository
                .findByDeviceIdAndUserUuidAndIsActiveTrue(finalDeviceId, user.getUuid())
                .orElse(null);

        // Créer ou mettre à jour la session
        com.xpertcash.entity.UserSession session;
        boolean isExistingSession = (existingSession != null);
        
        if (isExistingSession) {
            // Mettre à jour la session existante
            session = existingSession;
            session.updateLastActivity();
            session.setExpiresAt(LocalDateTime.now().plusYears(1)); // 1 an
        } else {
            // Limite de sessions actives : 2 par utilisateur
            final int MAX_ACTIVE_SESSIONS = 2;
            long activeSessionsCount = userSessionRepository.countByUserUuidAndIsActiveTrue(user.getUuid());
            
            if (activeSessionsCount >= MAX_ACTIVE_SESSIONS) {
                // L'utilisateur a déjà 2 sessions actives, on lui demande de choisir laquelle fermer
                throw new RuntimeException("SESSION_LIMIT_REACHED");
            }
            
            // Créer une nouvelle session
            session = new com.xpertcash.entity.UserSession();
            session.setUserUuid(user.getUuid());
            session.setUser(user);
            session.setDeviceId(finalDeviceId);
            // Détecter et améliorer le nom de l'appareil avec le modèle spécifique
            String enhancedDeviceName = deviceDetectionService.detectDeviceName(userAgent, deviceName);
            session.setDeviceName(enhancedDeviceName);
            session.setIpAddress(ipAddress);
            session.setUserAgent(userAgent);
            session.setCreatedAt(LocalDateTime.now());
            session.setLastActivity(LocalDateTime.now());
            session.setExpiresAt(LocalDateTime.now().plusYears(1)); // 1 an
            session.setActive(true);
        }

        // Charger l'admin et calculer within24Hours AVANT de sauvegarder
        // Optimisé : on charge l'admin seulement si nécessaire (évite de charger l'entreprise si pas besoin)
        User admin = null;
        try {
            admin = user.getEntreprise().getAdmin();
        } catch (Exception e) {
            // Si l'entreprise n'est pas chargée, la charger explicitement
            admin = usersRepository.findByUuid(user.getUuid())
                    .map(u -> u.getEntreprise().getAdmin())
                    .orElse(null);
        }
        boolean within24Hours = LocalDateTime.now().isBefore(user.getCreatedAt().plusHours(24));
        
        // Sauvegarder la session et générer le token en une seule fois
        // Gérer le cas où une session avec le même deviceId existe déjà (race condition)
        String accessToken = null;
        
        if (!isExistingSession) {
            // Pour une nouvelle session, on doit d'abord la sauvegarder pour obtenir l'ID
            // Vérifier une dernière fois avant de créer avec verrou pessimiste
            // Cela garantit qu'aucune autre requête ne peut créer une session entre temps
            Optional<com.xpertcash.entity.UserSession> lastCheck = userSessionRepository
                    .findByDeviceIdAndUserUuidAndIsActiveTrueWithLock(finalDeviceId, user.getUuid());
            
            if (lastCheck.isPresent()) {
                // Une session a été créée entre temps par une autre requête
                session = lastCheck.get();
                isExistingSession = true; // Traiter comme une session existante
            } else {
                // Aucune session n'existe, on peut créer
                try {
                    session = userSessionRepository.save(session);
                    // Maintenant qu'on a l'ID, générer le token
                    accessToken = generateAccessTokenWithSession(user, admin, within24Hours, session.getId());
                    // Mettre à jour le token avec une requête UPDATE directe (évite un deuxième save())
                    userSessionRepository.updateSessionToken(session.getId(), accessToken);
                    session.setSessionToken(accessToken); // Mettre à jour l'objet en mémoire aussi
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // Si une session avec le même deviceId existe déjà (contrainte unique violée)
                    Optional<com.xpertcash.entity.UserSession> existingSessionOpt = userSessionRepository
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
        
        // Si c'est une session existante (soit trouvée au début, soit récupérée)
        // et que le token n'a pas encore été généré
        if (isExistingSession && accessToken == null) {
            // Générer le token pour une session existante
            accessToken = generateAccessTokenWithSession(user, admin, within24Hours, session.getId());
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusYears(1);
            // Utiliser une requête UPDATE directe pour mettre à jour tout en une fois (optimisé)
            userSessionRepository.updateSessionActivityAndToken(session.getId(), now, expiresAt, accessToken);
            // Mettre à jour l'objet en mémoire pour la cohérence
            session.setSessionToken(accessToken);
            session.setLastActivity(now);
            session.setExpiresAt(expiresAt);
        }
        
        // S'assurer que le token a été généré (sécurité)
        if (accessToken == null) {
            throw new RuntimeException("Erreur : le token n'a pas pu être généré");
        }

        // NOTE: On ne met PAS à jour user.lastActivity ici pour éviter les deadlocks
        // La mise à jour de lastActivity est déjà gérée dans AuthenticationHelper lors des requêtes suivantes
        // Cela évite les verrous sur la table user pendant le login

    Map<String, String> tokens = new HashMap<>();
    tokens.put("accessToken", accessToken);
        tokens.put("deviceId", finalDeviceId); // Retourner le deviceId pour le frontend
    return tokens;
}

    // Méthode de compatibilité pour login sans paramètres de session
    public Map<String, String> login(String email, String password) {
        return login(email, password, null, null, null, null);
    }

            // Génération du token avec infos supplémentaires (sans sessionId - pour compatibilité)
            public String generateAccessToken(User user, User admin, boolean within24Hours) {
                return generateAccessTokenWithSession(user, admin, within24Hours, null);
            }

            // Génération du token avec sessionId pour gestion des sessions multiples
            public String generateAccessTokenWithSession(User user, User admin, boolean within24Hours, Long sessionId) {
            long expirationTime = 1000L * 60 * 60 * 24 * 365; // 1 an
            Date now = new Date();
            Date expirationDate = new Date(now.getTime() + expirationTime);

            boolean isAdminRole = user.getRole().getName().equals(RoleType.ADMIN)
                    || user.getRole().getName().equals(RoleType.SUPER_ADMIN);

            boolean userActivated = user.isEnabledLien();
            // Pour éviter les NullPointer (cas du SUPER_ADMIN ou entreprises sans admin défini)
            boolean adminActivated = (admin != null) ? admin.isEnabledLien() : true;
            boolean userActivationPossible = isAdminRole ? (user.isActivatedLien() || within24Hours) : true;

            // Inclure lastActivity dans le token pour invalider les anciens tokens lors du logout
            long lastActivityTimestamp = user.getLastActivity() != null 
                    ? user.getLastActivity().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    : now.getTime();

                var tokenBuilder = Jwts.builder()
                    .setSubject(user.getUuid())
                    .claim("role", user.getRole().getName())
                    .claim("userActivated", userActivated)
                    .claim("adminActivated", adminActivated)
                    .claim("userActivationPossible", userActivationPossible)
                    .claim("lastActivity", lastActivityTimestamp) // Version du token basée sur lastActivity
                    .setIssuedAt(now)
                        .setExpiration(expirationDate);

                // Ajouter le sessionId si fourni (pour gestion des sessions multiples)
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
        // Vérifier que l'utilisateur existe
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email."));

        // Vérifier si le compte est déjà activé
        if (user.isActivatedLien()) {
            throw new RuntimeException("Ce compte est déjà activé.");
        }

        // Essayer d'envoyer l'email d'activation
        try {
            mailService.sendActivationLinkEmail(user.getEmail(), user.getActivationCode(), user.getPersonalCode());
        } catch (MessagingException e) {
            System.err.println("Erreur lors du renvoi de l'email : " + e.getMessage());
            throw new RuntimeException("Impossible d'envoyer l'email pour le moment.");
        }
    }

    // Renvoyer l'email d'employé avec les identifiants (utilise le token sécurisé)
    @Transactional
    public void resendEmployeEmail(String email) {
        // Vérifier que l'utilisateur existe
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable avec cet email."));

        // Récupérer le token du mot de passe initial
        com.xpertcash.entity.PASSWORD.InitialPasswordToken token = initialPasswordTokenRepository
                .findByUser(user)
                .orElseThrow(() -> new RuntimeException("Impossible de renvoyer l'email : le token d'initialisation n'est plus disponible. L'utilisateur doit réinitialiser son mot de passe."));

        // Vérifier que le token n'est pas expiré
        if (token.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Le token d'initialisation a expiré. L'utilisateur doit réinitialiser son mot de passe.");
        }

        // Essayer d'envoyer l'email avec les identifiants
        try {
            mailService.sendEmployeEmail(
                user.getEmail(),
                user.getNomComplet(),
                user.getEntreprise().getNomEntreprise(),
                user.getRole().getName().toString(),
                user.getEmail(),
                token.getGeneratedPassword(), // Utiliser le mot de passe depuis le token
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

        // Activer le compte de l'utilisateur
        user.setActivatedLien(true);
        user.setEnabledLien(true);
        user = usersRepository.save(user);

        // Si c'est un ADMIN, activer tous ses employés
        if (user.getRole() != null && user.getRole().getName().equals(RoleType.ADMIN)) {
            List<User> usersToActivate = usersRepository.findByEntreprise(user.getEntreprise());
            usersToActivate.forEach(u -> u.setEnabledLien(true));
            usersRepository.saveAll(usersToActivate);
        }
        
        return user;
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

                User admin = authHelper.getAuthenticatedUserWithFallback(request);

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

                // 🎯 Stratégie optimale selon le type de rôle
                // - ADMIN/MANAGER : Utiliser le rôle template avec permissions par défaut
                // - UTILISATEUR et autres : Créer un rôle sans permissions (ajoutées plus tard)
                
                // Vérifier que le RoleType existe dans la base (validation)
                List<Role> existingRoles = roleRepository.findAllByName(userRequest.getRoleType());
                if (existingRoles.isEmpty()) {
                    throw new RuntimeException("Rôle invalide : " + userRequest.getRoleType() + ". Ce rôle n'existe pas dans la base de données.");
                }
                
                Role role;
                Long entrepriseId = admin.getEntreprise().getId();
                
                // Rôles avec permissions par défaut (ADMIN, MANAGER) : utiliser le template et cloner
                boolean isRoleWithDefaultPermissions = userRequest.getRoleType() == RoleType.ADMIN 
                        || userRequest.getRoleType() == RoleType.MANAGER;
                
                if (isRoleWithDefaultPermissions) {
                    // Pour ADMIN/MANAGER : chercher d'abord un rôle réutilisable dans la même entreprise
                    // avec les mêmes permissions par défaut
                    Role templateRole = existingRoles.get(0);
                    
                    // Vérifier si ce rôle template a des permissions (doit en avoir)
                    if (templateRole.getPermissions() == null || templateRole.getPermissions().isEmpty()) {
                        throw new RuntimeException("Le rôle " + userRequest.getRoleType() + " doit avoir des permissions par défaut.");
                    }
                    
                    // Chercher un rôle existant dans la même entreprise avec les mêmes permissions
                    // Réutilisation ILLIMITÉE : une entreprise peut créer autant d'utilisateurs qu'elle veut
                    Role reusableRole = null;
                    
                    // Extraire les PermissionType du template pour comparaison
                    Set<PermissionType> templatePermissionTypes = templateRole.getPermissions().stream()
                            .map(Permission::getType)
                            .collect(Collectors.toSet());
                    
                    for (Role r : existingRoles) {
                        // Vérifier si ce rôle est utilisé dans la même entreprise ou pas utilisé du tout
                        List<User> usersWithRoleInEntreprise = usersRepository.findByRoleAndEntrepriseId(r, entrepriseId);
                        List<User> allUsersWithRole = usersRepository.findByRole(r); // Tous les utilisateurs avec ce rôle (toutes entreprises)
                        boolean isUsedInSameEntreprise = !usersWithRoleInEntreprise.isEmpty();
                        boolean isNotUsedAnywhere = allUsersWithRole.isEmpty(); // Pas utilisé par aucune entreprise
                        
                        // Comparer les PermissionType (pas les objets Permission)
                        boolean hasSamePermissions = false;
                        if (r.getPermissions() != null && r.getPermissions().size() == templatePermissionTypes.size()) {
                            Set<PermissionType> rolePermissionTypes = r.getPermissions().stream()
                                    .map(Permission::getType)
                                    .collect(Collectors.toSet());
                            hasSamePermissions = rolePermissionTypes.equals(templatePermissionTypes);
                        }
                        
                        // Réutilisable si :
                        // 1. Mêmes permissions ET utilisé dans la même entreprise (réutilisation illimitée)
                        // 2. Mêmes permissions ET pas utilisé du tout (peut être réutilisé)
                        // Isolation : ne pas réutiliser un rôle utilisé par une autre entreprise
                        // Pas de limite : une entreprise peut créer autant d'utilisateurs qu'elle veut
                        if (hasSamePermissions && (isUsedInSameEntreprise || isNotUsedAnywhere)) {
                            reusableRole = r;
                            break;
                        }
                    }
                    
                    if (reusableRole != null) {
                        // Réutiliser un rôle existant avec les mêmes permissions dans la même entreprise
                        role = reusableRole;
                    } else {
                        // Créer un nouveau rôle cloné avec les permissions par défaut
                        role = new Role();
                        role.setName(templateRole.getName());
                        role.setPermissions(new ArrayList<>(templateRole.getPermissions())); // Copier les permissions
                        role = roleRepository.save(role);
                    }
                } else {
                    // Pour UTILISATEUR et autres : chercher un rôle réutilisable SANS permissions dans la même entreprise
                    Role reusableRole = null;
                    
                    for (Role r : existingRoles) {
                        List<User> usersWithRoleInEntreprise = usersRepository.findByRoleAndEntrepriseId(r, entrepriseId);
                        List<User> allUsersWithRole = usersRepository.findByRole(r); // Tous les utilisateurs avec ce rôle (toutes entreprises)
                        boolean hasNoPermissions = r.getPermissions() == null || r.getPermissions().isEmpty();
                        
                        // Réutilisation ILLIMITÉE : une entreprise peut créer autant d'utilisateurs qu'elle veut
                        // Réutilisable si : sans permissions ET (utilisé dans la même entreprise OU pas utilisé du tout)
                        boolean isUsedInSameEntreprise = !usersWithRoleInEntreprise.isEmpty();
                        boolean isNotUsedAnywhere = allUsersWithRole.isEmpty(); // Pas utilisé par aucune entreprise
                        
                        // Isolation : ne pas réutiliser un rôle utilisé par une autre entreprise
                        if (hasNoPermissions && (isUsedInSameEntreprise || isNotUsedAnywhere)) {
                            reusableRole = r;
                            break;
                        }
                    }
                    
                    if (reusableRole != null) {
                        // Réutiliser un rôle existant sans permissions
                        role = reusableRole;
                    } else {
                        // Créer un nouveau rôle sans permissions
                        role = new Role();
                        role.setName(userRequest.getRoleType());
                        role.setPermissions(new ArrayList<>()); // Liste vide : aucune permission par défaut
                        role = roleRepository.save(role);
                    }
                }

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


                // Enregistrer l'utilisateur
                User savedUser = usersRepository.save(newUser);

                // Créer un token pour le mot de passe initial (sécurisé)
                String initialPasswordTokenValue = UUID.randomUUID().toString();
                com.xpertcash.entity.PASSWORD.InitialPasswordToken initialPasswordToken = 
                    new com.xpertcash.entity.PASSWORD.InitialPasswordToken();
                initialPasswordToken.setToken(initialPasswordTokenValue);
                initialPasswordToken.setUser(savedUser);
                initialPasswordToken.setGeneratedPassword(generatedPassword);
                initialPasswordToken.setExpirationDate(LocalDateTime.now().plusDays(30)); // Valide 30 jours
                initialPasswordTokenRepository.save(initialPasswordToken);

                // Essayer d'envoyer l'email, mais ne pas interrompre l'inscription si ça échoue
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
                    System.err.println("⚠️ Erreur lors de l'envoi de l'email à " + savedUser.getEmail() + " : " + e.getMessage());
                    // Ne pas lancer d'exception, l'utilisateur est créé et l'email pourra être renvoyé plus tard
                }
                return savedUser;
            }

    //Attribution des permissions à un utilisateur
    @Transactional
    public UserDTO assignPermissionsToUser(Long userId, Map<PermissionType, Boolean> permissions, HttpServletRequest request) {
        User currentUser = authHelper.getAuthenticatedUserWithFallback(request);

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

        // ⚙️ Clonage du rôle si celui-ci est partagé par plusieurs utilisateurs
        Role targetRole = targetUser.getRole();
        List<User> usersWithSameRole = usersRepository.findByRole(targetRole);

        if (usersWithSameRole.size() > 1) {
            // Le rôle est partagé, on le duplique pour cet utilisateur uniquement
            Role clonedRole = new Role();
            clonedRole.setName(targetRole.getName());
            clonedRole.setPermissions(
                    targetRole.getPermissions() != null
                            ? new ArrayList<>(targetRole.getPermissions())
                            : new ArrayList<>()
            );

            clonedRole = roleRepository.save(clonedRole);

            // Assigner le nouveau rôle cloné à l'utilisateur cible
            targetUser.setRole(clonedRole);
            usersRepository.save(targetUser);

            targetRole = clonedRole;
        }

        // ⚙️ Mise à jour des permissions du rôle (désormais propre à l'utilisateur si cloné)
        List<Permission> existingPermissions = targetRole.getPermissions();
        if (existingPermissions == null) {
            existingPermissions = new ArrayList<>();
            targetRole.setPermissions(existingPermissions);
        }

        for (Map.Entry<PermissionType, Boolean> entry : permissions.entrySet()) {
            PermissionType permissionType = entry.getKey();
            Boolean isEnabled = entry.getValue();

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

        // 💾 Sauvegarde du rôle modifié
        roleRepository.save(targetRole);

        // Récupération des permissions du rôle mis à jour
        List<PermissionDTO> permissionsDTO = targetUser.getRole().getPermissions().stream()
                .map(permission -> new PermissionDTO(permission.getId(), permission.getType().toString()))
                .collect(Collectors.toList());

        // Création du RoleDTO avec les permissions
        RoleDTO roleDTO = new RoleDTO(targetUser.getRole().getId(), targetUser.getRole().getName().toString(), permissionsDTO);

        // Conversion de l'utilisateur en DTO et retour
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
            targetUser.getUserBoutiques().stream().map(UserBoutique::getId).collect(Collectors.toList()) // Adapté si tu veux juste les IDs des userBoutiques
        );
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

    // Empêcher de se supprimer soi-même
    if (admin.getId().equals(userId)) {
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
   
    // Supprimer les InitialPasswordToken de l'utilisateur AVANT de supprimer l'utilisateur
    // Cela évite l'erreur de contrainte de clé étrangère
    initialPasswordTokenRepository.deleteByUser(userToDelete);

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
            // Supprimer le token d'initialisation après le premier changement de mot de passe
            initialPasswordTokenRepository.deleteByUser(user);
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

        //Mise à jour du nom complet
       if (request.getNomComplet() != null) {
           user.setNomComplet(request.getNomComplet());
       }
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
        
        // --- 1. Récupérer l'utilisateur avec entreprise et role join fetch ---
        User user = usersRepository.findByIdWithEntrepriseAndRole(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

    Entreprise entreprise = user.getEntreprise();

    List<BoutiqueResponse> boutiqueResponses;
    String roleType = user.getRole().getName().name();

    if ("VENDEUR".equals(roleType)) {
        // Boutiques assignées au vendeur
        boutiqueResponses = user.getUserBoutiques()
                .stream()
                .map(ub -> {
                    Boutique b = ub.getBoutique();
                    return new BoutiqueResponse(
                            b.getId(),
                            b.getNomBoutique(),
                            b.getAdresse(),
                            b.getTelephone(),
                            b.getEmail(),
                            b.getCreatedAt(),
                            b.isActif(),
                            b.getTypeBoutique()
                    );
                })
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
            .map(p -> p.getType().name())
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
    User user = authHelper.getAuthenticatedUserWithFallback(request);

    // Récupération de l'entreprise associée à l'utilisateur
    Entreprise entreprise = user.getEntreprise();
    if (entreprise == null) {
        throw new RuntimeException("Aucune entreprise associée à cet utilisateur.");
    }

    // Vérification des permissions de l'utilisateur
    boolean isAdminOrManager = CentralAccess.isAdminOrManagerOfEntreprise(user, entreprise.getId());
    boolean hasPermission = user.getRole().hasPermission(PermissionType.GERER_UTILISATEURS);
    boolean isComptable = user.getRole().hasPermission(PermissionType.VOIR_FLUX_COMPTABLE);
   

    

    // Vérification des droits d'accès
    if (!isAdminOrManager && !hasPermission && isComptable) {
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
            userBoutiques
        );
    }).collect(Collectors.toList());
}


    //Get user by id
  public UserDTO getUserById(Long userId, HttpServletRequest request) {

    User connectedUser = authHelper.getAuthenticatedUserWithFallback(request);

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

    boolean isSelf = connectedUser.getId().equals(userId);

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
        userBoutiquesIds
    );

    return userDTO;
}

    // Méthode pour suspendre ou réactiver un utilisateur
    @Transactional
    public void suspendUser(HttpServletRequest request, Long userId, boolean suspend) {
        User currentUser = authHelper.getAuthenticatedUserWithFallback(request);

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
        User user = authHelper.getAuthenticatedUserWithFallback(request);

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
    User user = authHelper.getAuthenticatedUserWithFallback(request);

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

    @Transactional(readOnly = true)
    public UserOptimalDTO getDashboardData(HttpServletRequest request) {
        // 🔐 Récupération de l'utilisateur connecté
        User currentUser = authHelper.getAuthenticatedUserWithFallback(request);

        if (currentUser.getEntreprise() == null) {
            throw new RuntimeException("Vous n'êtes associé à aucune entreprise.");
        }

        Long entrepriseId = currentUser.getEntreprise().getId();

        // 1. Informations de l'utilisateur connecté
        UserOptimalDTO.UserInfoDTO userInfo = new UserOptimalDTO.UserInfoDTO();
        userInfo.setId(currentUser.getId());
        userInfo.setNomComplet(currentUser.getNomComplet());
        userInfo.setEmail(currentUser.getEmail());
        userInfo.setRoleType(currentUser.getRole() != null ? currentUser.getRole().getName().toString() : null);
        userInfo.setUuid(currentUser.getUuid());
        userInfo.setPays(currentUser.getPays());
        userInfo.setPhone(currentUser.getPhone());

        // 2. Tous les rôles disponibles
        List<UserOptimalDTO.RoleDTO> roles = roleRepository.findAll().stream()
                .map(role -> {
                    UserOptimalDTO.RoleDTO roleDTO = new UserOptimalDTO.RoleDTO();
                    roleDTO.setId(role.getId());
                    roleDTO.setName(role.getName().toString());
                    roleDTO.setDescription(getRoleDescription(role.getName()));
                    return roleDTO;
                })
                .collect(Collectors.toList());

        // 3. Toutes les boutiques de l'entreprise
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

        // 4. Tous les utilisateurs de l'entreprise
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

                    // Ajouter les détails du rôle
                    if (user.getRole() != null) {
                        UserOptimalDTO.RoleDTO roleDTO = new UserOptimalDTO.RoleDTO();
                        roleDTO.setId(user.getRole().getId());
                        roleDTO.setName(user.getRole().getName().toString());
                        roleDTO.setDescription(getRoleDescription(user.getRole().getName()));
                        userDTO.setRole(roleDTO);

                        // Ajouter les permissions du rôle
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

        // 5. Rôle de l'utilisateur connecté
        String currentUserRole = currentUser.getRole() != null ? currentUser.getRole().getName().toString() : null;

        // Construire et retourner le DTO
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
        List<com.xpertcash.entity.UserSession> sessions = userSessionRepository.findByUserUuidAndIsActiveTrue(userUuid);
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
                    dto.setCurrentSession(false); // On ne peut pas déterminer la session courante sans token
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    // Méthode pour trouver un utilisateur par email (pour récupérer les sessions avant login)
    public User findUserByEmail(String email) {
        return usersRepository.findByEmail(email).orElse(null);
    }
    
    // Méthode pour vérifier le mot de passe (pour sécurité lors de la récupération des sessions)
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    // Méthode pour fermer une session spécifique avant login (quand limite atteinte)
    @Transactional
    public void closeSessionBeforeLogin(String email, String password, Long sessionId) {
        // Vérifier l'email et le mot de passe
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }
        
        // Vérifier que la session appartient à cet utilisateur
        com.xpertcash.entity.UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session introuvable"));
        
        if (!session.getUserUuid().equals(user.getUuid())) {
            throw new RuntimeException("Cette session ne vous appartient pas");
        }
        
        // Supprimer la session
        userSessionRepository.delete(session);
    }
    
    public List<com.xpertcash.DTOs.UserSessionDTO> getActiveSessions(HttpServletRequest request) {
        User user = authHelper.getAuthenticatedUserWithFallback(request);
        
        // Récupérer toutes les sessions actives de l'utilisateur
        List<com.xpertcash.entity.UserSession> sessions = userSessionRepository
                .findByUserUuidAndIsActiveTrue(user.getUuid());
        
        // Extraire le sessionId du token courant pour identifier la session actuelle
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
        
        // Convertir en DTO
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
        com.xpertcash.entity.UserSession session = userSessionRepository.findById(sessionId)
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
        
        // Extraire le sessionId du token courant
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
        
        // Révoquer toutes les sessions sauf la session courante
        userSessionRepository.revokeAllSessionsExcept(user.getUuid(), currentSessionId);
    }

}
