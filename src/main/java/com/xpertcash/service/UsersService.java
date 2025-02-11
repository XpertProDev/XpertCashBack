package com.xpertcash.service;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.repository.EntrepriseRepository;
import com.xpertcash.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UsersService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private MailService mailService;

    // Inscription : génère le code PIN, enregistre l'utilisateur et envoie le lien d'activation
    public User registerUsers(String nomComplet, String email, String password, String phone, String nomEntreprise) {
        // Vérifier si l'email est déjà utilisé
        if (usersRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé.");
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

        // Créer l'utilisateur
        User users = new User();
        users.setEmail(email);
        users.setPassword(hashedPassword);
        users.setPhone(phone);
        users.setNomComplet(nomComplet);
        users.setEntreprise(entreprise); // Associer l'entreprise à l'utilisateur
        users.setActivationCode(activationCode);
        users.setCreatedAt(LocalDateTime.now());
        users.setActivatedLien(false);
        users.setEnabledLien(true);
        users.setLastActivity(LocalDateTime.now());
        users.setLocked(false);
        usersRepository.save(users);

        try {
            mailService.sendActivationLinkEmail(email, activationCode);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi de l'email d'activation : " + e.getMessage());
        }

        return users;
    }

    // Connexion : vérifie l'état du compte et met à jour la dernière activité
    public void login(String email, String password) {
        User users = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // Vérification du mot de passe haché
        if (!passwordEncoder.matches(password, users.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect.");
        }

        // Vérification si le compte est verrouillé
        if (users.isLocked()) {
            throw new RuntimeException("Votre compte est verrouillé pour inactivité. Veuillez le déverrouiller via le lien envoyé par email.");
        }

        // Vérification de l'activation du compte
        if (!users.isActivatedLien()) {
            LocalDateTime expiration = users.getCreatedAt().plusHours(24);
            if (LocalDateTime.now().isAfter(expiration)) {
                users.setEnabledLien(false);
                usersRepository.save(users);
                throw new RuntimeException("Votre période d'utilisation gratuite de 24h est terminée et votre compte a été désactivé. Veuillez activer votre compte via le lien envoyé par email.");
            }
        }

        // Mise à jour de la dernière activité
        users.setLastActivity(LocalDateTime.now());
        usersRepository.save(users);
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
