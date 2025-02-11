package com.xpertcash.service.PASSWORD.resetPassword;

import com.xpertcash.DTOs.PASSWORD.resetPassword.PasswordResetConfirmation;
import com.xpertcash.DTOs.PASSWORD.resetPassword.PasswordResetRequest;
import com.xpertcash.entity.User;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordResetService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private MailService mailService;

    // Stockage temporaire des demandes de réinitialisation (email -> PendingPasswordReset)
    private Map<String, PendingPasswordReset> pendingPasswordResets = new ConcurrentHashMap<>();

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Demande de réinitialisation de mot de passe.
     * Vérifie que l'utilisateur existe, génère un code de vérification et l'envoie à l'email fourni.
     */
    public void requestPasswordReset(PasswordResetRequest request) {
        String email = request.getEmail();
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aucun utilisateur trouvé avec cet email."));

        // Générer un code de vérification à 6 chiffres
        String verificationCode = String.format("%06d", new Random().nextInt(1000000));

        // Enregistrer la demande de réinitialisation dans la map
        PendingPasswordReset pendingReset = new PendingPasswordReset(verificationCode, LocalDateTime.now());
        pendingPasswordResets.put(email.toLowerCase(), pendingReset);

        // Envoyer le code de vérification à l'email
        try {
            mailService.sendEmailVerificationCode(email, verificationCode);
        } catch (MailException e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email de vérification : " + e.getMessage());
        }
    }

    /**
     * Confirmation de la réinitialisation de mot de passe.
     * Vérifie le code de vérification et, s'il est correct, met à jour le mot de passe de l'utilisateur.
     */
    public void confirmPasswordReset(PasswordResetConfirmation confirmation) {
        String email = confirmation.getEmail().toLowerCase();
        PendingPasswordReset pendingReset = pendingPasswordResets.get(email);
        if (pendingReset == null) {
            throw new RuntimeException("Aucune demande de réinitialisation trouvée pour cet email.");
        }

        // Ici, vous pouvez ajouter une vérification d'expiration en comparant pendingReset.getRequestedAt() avec l'heure actuelle

        // Vérifier que le code fourni correspond
        if (!pendingReset.getVerificationCode().equals(confirmation.getVerificationCode())) {
            throw new RuntimeException("Code de vérification invalide.");
        }

        // Mettre à jour le mot de passe de l'utilisateur
        User user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé."));
        user.setPassword(passwordEncoder.encode(confirmation.getNewPassword()));
        usersRepository.save(user);

        // Supprimer la demande de réinitialisation une fois le mot de passe mis à jour
        pendingPasswordResets.remove(email);
    }

    // Classe interne pour stocker la demande de réinitialisation en attente
    private static class PendingPasswordReset {
        private final String verificationCode;
        private final LocalDateTime requestedAt;

        public PendingPasswordReset(String verificationCode, LocalDateTime requestedAt) {
            this.verificationCode = verificationCode;
            this.requestedAt = requestedAt;
        }

        public String getVerificationCode() {
            return verificationCode;
        }

        public LocalDateTime getRequestedAt() {
            return requestedAt;
        }
    }
}
