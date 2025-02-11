package com.xpertcash.service.PASSWORD;

import com.xpertcash.DTOs.PASSWORD.PasswordUpdateConfirmationRequest;
import com.xpertcash.DTOs.PASSWORD.PasswordUpdateRequest;
import com.xpertcash.entity.User;
import com.xpertcash.repository.UsersRepository;
import com.xpertcash.service.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordUpdateService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private MailService mailService;

    // Stockage temporaire des demandes de mise à jour (userId -> PendingPasswordUpdate)
    private Map<Long, PendingPasswordUpdate> pendingPasswordUpdates = new ConcurrentHashMap<>();

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Demande de mise à jour du mot de passe.
     * L'utilisateur fournit son mot de passe actuel et le nouveau mot de passe.
     * Le service vérifie que le mot de passe actuel est correct, puis génère un code de vérification
     * et l'envoie à l'email actuel de l'utilisateur.
     */
    public void requestPasswordUpdate(Long userId, PasswordUpdateRequest request) {
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Vérifier que le mot de passe actuel fourni est correct
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Le mot de passe actuel est incorrect.");
        }

        // Vérifier que le nouveau mot de passe est différent du mot de passe actuel
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("Vous utilisez déjà ce mot de passe.");
        }

        // Générer un code de vérification à 6 chiffres
        String verificationCode = String.format("%06d", new Random().nextInt(1000000));

        // Enregistrer la demande en attente
        PendingPasswordUpdate pendingUpdate = new PendingPasswordUpdate(request.getNewPassword(), verificationCode, LocalDateTime.now());
        pendingPasswordUpdates.put(userId, pendingUpdate);

        // Envoyer le code de vérification à l'email actuel de l'utilisateur
        mailService.sendEmailVerificationCode(user.getEmail(), verificationCode);
    }

    /**
     * Confirmation de la mise à jour du mot de passe.
     * L'utilisateur fournit le code de vérification reçu par email.
     * Si le code est correct, le mot de passe est mis à jour.
     */
    public void confirmPasswordUpdate(Long userId, PasswordUpdateConfirmationRequest confirmationRequest) {
        PendingPasswordUpdate pendingUpdate = pendingPasswordUpdates.get(userId);
        if (pendingUpdate == null) {
            throw new RuntimeException("Aucune demande de mise à jour de mot de passe n'a été trouvée pour cet utilisateur.");
        }

        // Vérifier que le code de vérification correspond
        if (!pendingUpdate.getVerificationCode().equals(confirmationRequest.getVerificationCode())) {
            throw new RuntimeException("Code de vérification invalide.");
        }

        // Mettre à jour le mot de passe de l'utilisateur (après hachage)
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setPassword(passwordEncoder.encode(pendingUpdate.getNewPassword()));
        usersRepository.save(user);

        // Supprimer la demande en attente
        pendingPasswordUpdates.remove(userId);
    }

    // Classe interne pour stocker la demande en attente
    private static class PendingPasswordUpdate {
        private String newPassword;
        private String verificationCode;
        private LocalDateTime requestedAt;

        public PendingPasswordUpdate(String newPassword, String verificationCode, LocalDateTime requestedAt) {
            this.newPassword = newPassword;
            this.verificationCode = verificationCode;
            this.requestedAt = requestedAt;
        }

        public String getNewPassword() {
            return newPassword;
        }
        public String getVerificationCode() {
            return verificationCode;
        }
        public LocalDateTime getRequestedAt() {
            return requestedAt;
        }
    }
}
