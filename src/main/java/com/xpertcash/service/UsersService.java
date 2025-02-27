package com.xpertcash.service;

import com.xpertcash.DTOs.UpdateUserRequest;
import com.xpertcash.DTOs.USER.UserRequest;
import com.xpertcash.configuration.JwtConfig;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.configuration.PasswordGenerator;
import com.xpertcash.entity.*;
import com.xpertcash.repository.BoutiqueRepository;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.ProduitRepository;
import com.xpertcash.repository.RoleRepository;
import com.xpertcash.repository.StockRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.transaction.Transactional;


import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

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
    private JwtUtil jwtUtil;  // Utilisation de JwtUtil pour extraire l'ID de l'utilisateur

    private final JwtConfig jwtConfig;
    @Autowired
    private BoutiqueRepository boutiqueRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private ProduitRepository produitRepository;


    @Autowired
    public UsersService(UsersRepository usersRepository, JwtConfig jwtConfig, BCryptPasswordEncoder passwordEncoder) {
        this.usersRepository = usersRepository;
        this.jwtConfig = jwtConfig;
        this.passwordEncoder = passwordEncoder;
    }

    // Inscription : génère le code PIN, enregistre l'utilisateur et envoie le lien d'activation
    public User registerUsers(String nomComplet, String email, String password, String phone, String pays, String nomEntreprise, String nomBoutique) {
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
    
        // Générer le code PIN d'activation
        String activationCode = String.format("%04d", new Random().nextInt(10000));
    
        // Créer l'entreprise
        String identifiantUnique;
        do {
            identifiantUnique = Entreprise.generateIdentifiantEntreprise();
        } while (entrepriseRepository.existsByIdentifiantEntreprise(identifiantUnique));
    
        Entreprise entreprise = new Entreprise();
        entreprise.setNomEntreprise(nomEntreprise);
        entreprise.setIdentifiantEntreprise(identifiantUnique);
        entreprise.setCreatedAt(LocalDateTime.now());
        entreprise = entrepriseRepository.save(entreprise);
    
        // Créer la boutique (magasin) associée à l'entreprise
        Boutique boutique = new Boutique();
        boutique.setNomBoutique(nomBoutique);
        boutique.setEntreprise(entreprise);
        boutique.setCreatedAt(LocalDateTime.now());
        boutiqueRepository.save(boutique);
    
        // Créer un stock vide initial, sans produit
        Stock stock = new Stock();
        stock.setQuantite(0);  // Stock vide
        stock.setBoutique(boutique);
        stock.setCreatedAt(LocalDateTime.now());
        stock.setLastUpdated(LocalDateTime.now());
        stockRepository.save(stock);  // Aucune association de produit au stock ici
    
        // Attribution du rôle ADMIN à l'utilisateur
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
        user.setCreatedAt(LocalDateTime.now());
        user.setActivatedLien(false);
        user.setEnabledLien(true);
        user.setLastActivity(LocalDateTime.now());
        user.setLocked(false);
        usersRepository.save(user);
    
        // Assigner l'utilisateur admin à l'entreprise
        entreprise.setAdmin(user);
        entrepriseRepository.save(entreprise);
    
        // Envoi de l'email d'activation avec le code PIN
        try {
            mailService.sendActivationLinkEmail(email, activationCode);
        } catch (MessagingException e) {
            System.err.println("Erreur lors de l'envoi de l'email d'activation : " + e.getMessage());
            throw new RuntimeException("Une erreur est survenue lors de l'envoi de l'email d'activation.", e);
        }
    
        // Retourner l'utilisateur créé
        return user;
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

    // Connexion : vérifie l'état du compte et retourne un token JWT
    public String login(String email, String password) {
                // Récupérer l'utilisateur par son email
                User user = usersRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

                // Vérification du mot de passe
                if (!passwordEncoder.matches(password, user.getPassword())) {
                    throw new RuntimeException("Mot de passe incorrect.");
                }

                // Vérification si le compte est verrouillé
                if (user.isLocked()) {
                    throw new RuntimeException("Votre compte est verrouillé pour inactivité.");
                }

                // Récupérer l'admin associé à l'entreprise de l'utilisateur
                User admin = user.getEntreprise().getAdmin();

                // Vérifier si l'utilisateur est ADMIN
                boolean isAdmin = user.getRole().getName().equals(RoleType.ADMIN);

                // Vérifier si le compte a été créé il y a moins de 24h
                LocalDateTime expirationDate = user.getCreatedAt().plusHours(24);
                //LocalDateTime expirationDate = user.getCreatedAt().plusMinutes(5);
                boolean within24Hours = LocalDateTime.now().isBefore(expirationDate);

                // **Cas 1 : ADMIN**
                if (isAdmin) {
                    if (!user.isActivatedLien() && !within24Hours) {
                        throw new RuntimeException("Votre compte administrateur n'est pas activé et la période de connexion temporaire est expirée.");
                    }
                } else {
                    // **Cas 2 : EMPLOYÉ**
                    if (!admin.isActivatedLien()) {
                        if (!within24Hours) {
                            throw new RuntimeException("Votre compte est désactivé car votre administrateur n'a pas activé son compte.");
                        }
                    }
                }

                // Si tout est OK, mise à jour de la dernière activité
                user.setLastActivity(LocalDateTime.now());
                usersRepository.save(user);

                // Générer et retourner le JWT
                return generateToken(user);
            }

    // Génèration du token
    private String generateToken(User user) {
        long expirationTime = 1000 * 60 * 60 * 24; // 24 heures
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationTime);

        // Générer le token JWT
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))  // L'ID de l'utilisateur comme "subject"
                .claim("role", user.getRole().getName())  // Ajouter le rôle de l'utilisateur dans le claim
                .setIssuedAt(now)  // Date de création du token
                .setExpiration(expirationDate)  // Date d'expiration du token
                .signWith(SignatureAlgorithm.HS256, jwtConfig.getSecretKey())  // Utilisation de la clé secrète de JwtConfig pour signer
                .compact();
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

                // Vérifier que l'Admin est bien un ADMIN
                if (admin.getRole() == null || !admin.getRole().getName().equals(RoleType.ADMIN)) {
                    throw new RuntimeException("Seul un ADMIN peut ajouter des utilisateurs !");
                }

                // Vérifier que l'Admin possède une entreprise
                if (admin.getEntreprise() == null) {
                    throw new RuntimeException("L'Admin n'a pas d'entreprise associée.");
                }

                // Vérifier si un utilisateur avec le même email ou téléphone existe déjà
                if (usersRepository.findByEmailAndEntreprise(userRequest.getEmail(), admin.getEntreprise()).isPresent()) {
                    throw new RuntimeException("Un utilisateur avec cet email existe déjà dans votre entreprise.");
                }

                if (usersRepository.findByPhoneAndEntreprise(userRequest.getPhone(), admin.getEntreprise()).isPresent()) {
                    throw new RuntimeException("Un utilisateur avec ce numéro de téléphone existe déjà dans votre entreprise.");
                } 

                if (usersRepository.findByPhoneAndEntrepriseAndPays(userRequest.getPhone(), admin.getEntreprise(), userRequest.getPays()).isPresent()) {
                    throw new RuntimeException("Un utilisateur avec ce numéro de téléphone existe déjà dans votre entreprise.");
                }
                

                

                // Vérifier que le rôle spécifié pour le nouvel utilisateur existe
                Role role = roleRepository.findByName(userRequest.getRoleType())
                        .orElseThrow(() -> new RuntimeException("Rôle invalide : " + userRequest.getRoleType()));

                // Générer un mot de passe et l'encoder
                String generatedPassword = PasswordGenerator.generatePassword();
                String encodedPassword = passwordEncoder.encode(generatedPassword);

                // Créer un nouvel utilisateur avec l'activation dépendante de l'admin
                User newUser = new User();
                newUser.setEmail(userRequest.getEmail());
                newUser.setPassword(encodedPassword);
                newUser.setNomComplet(userRequest.getNomComplet());
                newUser.setPays(userRequest.getPays());
                newUser.setPhone(userRequest.getPhone());
                newUser.setEnabledLien(admin.isActivatedLien()); // L'employé est activé SEULEMENT si l'admin est activé
                newUser.setCreatedAt(LocalDateTime.now());
                newUser.setEntreprise(admin.getEntreprise());
                newUser.setRole(role);

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
                        generatedPassword
                    );
                } catch (MessagingException e) {
                    System.err.println("Erreur lors de l'envoi de l'email à " + savedUser.getEmail() + " : " + e.getMessage());
                    e.printStackTrace();
                    throw new RuntimeException("Utilisateur créé mais une erreur est survenue lors de l'envoi de l'email.", e);
                }
                
                


                return savedUser;
            }

    // Pour la modification de utilisateur
    @Transactional
    public User updateUser(Long userId, UpdateUserRequest request) {
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Si le numéro de téléphone a été modifié, faire la vérification
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            // Vérifier si le numéro de téléphone est déjà utilisé par un autre utilisateur
            Optional<User> existingUserWithPhone = usersRepository.findByPhone(request.getPhone());
            if (existingUserWithPhone.isPresent() && !existingUserWithPhone.get().getId().equals(userId)) {
                throw new RuntimeException("Ce numéro de téléphone est déjà utilisé par un autre utilisateur.");
            }
        }

        // Si le nom a changé, le mettre à jour
        if (request.getNomComplet() != null) {
            user.setNomComplet(request.getNomComplet());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        usersRepository.save(user);
        return user;
    }

    public UserRequest getInfo(Long userId) {
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return new UserRequest(
                user.getNomComplet(),
                user.getEntreprise().getNomEntreprise(),
                user.getEmail(),
                user.getRole().getName(),
                user.getPhone(),
                user.getPays(),
                user.getEntreprise().getAdresse(),
                user.getEntreprise().getLogo()

        );
    }

}
