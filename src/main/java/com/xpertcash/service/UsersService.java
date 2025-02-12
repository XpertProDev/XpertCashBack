package com.xpertcash.service;

import com.xpertcash.DTOs.UpdateUserRequest;
import com.xpertcash.DTOs.USER.UserRequest;
import com.xpertcash.configuration.PasswordGenerator;
import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Role;
import com.xpertcash.entity.RoleType;
import com.xpertcash.entity.User;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.RoleRepository;
import com.xpertcash.repository.UsersRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
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


    // Inscription : génère le code PIN, enregistre l'utilisateur et envoie le lien d'activation
    public User registerUsers(String nomComplet, String email, String password, String phone, String nomEntreprise) {
        // Vérifier si l'email est déjà utilisé
        if (usersRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé.");
        }

        // Vérifier si le numéro de téléphone est déjà utilisé
        if (usersRepository.findByPhone(phone).isPresent()) {
            throw new RuntimeException("Ce numéro de téléphone est déjà utilisé. Veuillez en choisir un autre.");
        }

        // Vérifier si l'entreprise existe déjà
        if (entrepriseRepository.findByNomEntreprise(nomEntreprise).isPresent()) {
            throw new RuntimeException("Le nom de l'entreprise est déjà utilisé. Veuillez choisir un autre nom.");
        }

        // Générer un mot de passe haché
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(password);

        // Générer un code PIN d'activation
        String activationCode = String.format("%04d", new Random().nextInt(10000));

        // Générer un identifiant unique pour l'entreprise
        String identifiantUnique;
        do {
            identifiantUnique = Entreprise.generateIdentifiantEntreprise();
        } while (entrepriseRepository.existsByIdentifiantEntreprise(identifiantUnique));

        // Créer l'entreprise
        Entreprise entreprise = new Entreprise();
        entreprise.setNomEntreprise(nomEntreprise);
        entreprise.setIdentifiantEntreprise(identifiantUnique);
        entreprise.setCreatedAt(LocalDateTime.now());
        entreprise = entrepriseRepository.save(entreprise);

        // Attribution du rôle ADMIN
                Role adminRole = roleRepository.findByName(RoleType.ADMIN)
        .orElseThrow(() -> new RuntimeException("Rôle ADMIN non trouvé"));

        // Créer l'utilisateur
        User users = new User();
        users.setEmail(email);
        users.setPassword(hashedPassword);
        users.setPhone(phone);
        users.setNomComplet(nomComplet);
        users.setEntreprise(entreprise);
        users.setRole(adminRole);
        users.setActivationCode(activationCode);
        users.setCreatedAt(LocalDateTime.now());
        users.setActivatedLien(false);
        users.setEnabledLien(true);
        users.setLastActivity(LocalDateTime.now());
        users.setLocked(false);
        usersRepository.save(users);

        entreprise.setAdmin(users);
        entrepriseRepository.save(entreprise);

        try {
            mailService.sendActivationLinkEmail(email, activationCode);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email d'activation : " + e.getMessage());
        }

        return users;
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
    



    // Connexion : vérifie l'état du compte et met à jour la dernière activité
                public void login(String email, String password) {
                    User user = usersRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

                    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

                    // Vérification du mot de passe
                    if (!passwordEncoder.matches(password, user.getPassword())) {
                        throw new RuntimeException("Mot de passe incorrect.");
                    }

                    // Vérification si le compte est verrouillé
                    if (user.isLocked()) {
                        throw new RuntimeException("Votre compte est verrouillé pour inactivité. Veuillez le déverrouiller via le lien envoyé par email.");
                    }

                    // Vérification de l'activation du compte
                    if (!user.isActivatedLien()) {
                        LocalDateTime expiration = user.getCreatedAt().plusHours(24);
                        if (LocalDateTime.now().isAfter(expiration)) {
                            user.setEnabledLien(false);
                            usersRepository.save(user);
                            throw new RuntimeException("Votre période d'utilisation gratuite de 24h est terminée et votre compte a été désactivé.");
                        }
                    }

                    // Mise à jour de la dernière activité
                    user.setLastActivity(LocalDateTime.now());
                    usersRepository.save(user);

                    // Ajout d'une gestion selon le rôle (exemple)
                    if (user.getRole().getName().equals("ADMIN")) {
                        System.out.println("L'utilisateur est un ADMIN.");
                    } else if (user.getRole().getName().equals("VENDEUR")) {
                        System.out.println("L'utilisateur est un VENDEUR.");
                    }
                }





    // Activation du compte via le lien d'activation (email + code PIN)
    public void activateAccount(String email, String code) {
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (user.getActivationCode().equals(code)) {
            user.setActivatedLien(true);
            user.setEnabledLien(true); // Réactive le compte si celui-ci avait été désactivé
            usersRepository.save(user);
        } else {
            throw new RuntimeException("Code d'activation invalide.");
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
    public User addUserToEntreprise(Long adminId, UserRequest request) {
        // Récupérer l'admin par l'ID fourni dans l'URL
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
    
        // Vérifier si un utilisateur avec le même email ou le même téléphone existe déjà dans l'entreprise
        Optional<User> existingUserByEmail = usersRepository.findByEmailAndEntreprise(request.getEmail(), admin.getEntreprise());
        Optional<User> existingUserByPhone = usersRepository.findByPhoneAndEntreprise(request.getPhone(), admin.getEntreprise());
    
        if (existingUserByEmail.isPresent()) {
            throw new RuntimeException("Un utilisateur avec cet email existe déjà dans votre entreprise.");
        }
    
        if (existingUserByPhone.isPresent()) {
            throw new RuntimeException("Un utilisateur avec ce numéro de téléphone existe déjà dans votre entreprise.");
        }
    
        // Vérifier que le rôle spécifié pour le nouvel utilisateur existe
        Role role = roleRepository.findByName(request.getRoleType())
                .orElseThrow(() -> new RuntimeException("Rôle invalide : " + request.getRoleType()));
    
        // Générer un mot de passe pour l'utilisateur
        String generatedPassword = PasswordGenerator.generatePassword();
        
        // Encoder le mot de passe avant de l'enregistrer
        String encodedPassword = passwordEncoder.encode(generatedPassword);
    
        // Créer un nouvel utilisateur avec le mot de passe encodé
        User newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(encodedPassword); // Utiliser le mot de passe encodé
        newUser.setNomComplet(request.getNomComplet());
        newUser.setPhone(request.getPhone());
        newUser.setEnabledLien(admin.isEnabledLien());
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setEntreprise(admin.getEntreprise());
        newUser.setRole(role);
    
        // Enregistrer l'utilisateur en base de données
        User savedUser = usersRepository.save(newUser);
    
       // Envoi de l'email avec les informations
        String message = String.format(
            "Bonjour %s,\n\n" +
            "Vous venez d'être ajouté à l'entreprise %s en tant que %s.\n\n" +
            "Voici vos identifiants :\n" +
            "Email : %s\n" +
            "Mot de passe : %s\n\n" +
            "Merci.",
            savedUser.getNomComplet(),                       
            savedUser.getEntreprise().getNomEntreprise(),             
            savedUser.getRole().getName(),               
            savedUser.getEmail(),                         
            generatedPassword                            
        );

    
        try {
            mailService.sendEmail(savedUser.getEmail(), "Création de votre compte", message);
        } catch (MailException e) {
            throw new RuntimeException("Utilisateur créé mais une erreur est survenue lors de l'envoi de l'email.");
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







    // Vérification (par exemple via un scheduler ou un intercepteur) de l'inactivité supérieure à 30 minutes
    // Si c'est le cas, le compte est verrouillé et un lien de déverrouillage est envoyé par email
    /*public void checkAndLockIfInactive(Users users) {
        if (!users.isLocked() && users.getLastActivity() != null &&
                users.getLastActivity().plusMinutes(30).isBefore(LocalDateTime.now())) {
            users.setLocked(true);
            usersRepository.save(users);
            mailService.sendUnlockLinkEmail(users.getEmail(), users.getActivationCode());
        }
    }*/
}
