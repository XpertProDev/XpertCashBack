package com.xpertcash.service;

import com.xpertcash.DTOs.EmailUpdateConfirmationRequest;
import com.xpertcash.DTOs.EmailUpdateRequest;
import com.xpertcash.entity.User;
import com.xpertcash.repository.UsersRepository;

import jakarta.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailUpdateService {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private MailService mailService;

    private Map<Long, PendingEmailUpdate> pendingEmailUpdates = new ConcurrentHashMap<>();

    /**
     * Demande de mise à jour d'email.
     */
    public void requestEmailUpdate(Long userId, EmailUpdateRequest request) {
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (user.getEmail().equalsIgnoreCase(request.getNewEmail())) {
            throw new RuntimeException("Vous utilisez déjà cet email.");
        }

        Long entrepriseId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;
        if (entrepriseId == null) {
            throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
        }
        if (usersRepository.findByEmailAndEntrepriseId(request.getNewEmail(), entrepriseId).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé par un autre utilisateur dans votre entreprise.");
        }

        String verificationCode = String.format("%06d", new Random().nextInt(1000000));

        PendingEmailUpdate pendingUpdate = new PendingEmailUpdate(request.getNewEmail(), verificationCode, LocalDateTime.now());
        pendingEmailUpdates.put(userId, pendingUpdate);

                try {
            mailService.sendEmailVerificationCode(user.getEmail(), verificationCode);
        } catch (MessagingException e) {
            System.err.println("Erreur lors de l'envoi du code de vérification à l'email : " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi du code de vérification. L'email n'a pas pu être envoyé.");
        }

    }

    /**
     * Confirmation de la mise à jour d'email.
     */
    public void confirmEmailUpdate(Long userId, EmailUpdateConfirmationRequest confirmationRequest) {
        PendingEmailUpdate pendingUpdate = pendingEmailUpdates.get(userId);
        if (pendingUpdate == null) {
            throw new RuntimeException("Aucune demande de mise à jour d'email n'a été trouvée pour cet utilisateur.");
        }


        if (!pendingUpdate.getVerificationCode().equals(confirmationRequest.getVerificationCode())) {
            throw new RuntimeException("Code de vérification invalide.");
        }

        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setEmail(pendingUpdate.getNewEmail());
        usersRepository.save(user);

        pendingEmailUpdates.remove(userId);
    }

    private static class PendingEmailUpdate {
        private String newEmail;
        private String verificationCode;
        private LocalDateTime requestedAt;

        public PendingEmailUpdate(String newEmail, String verificationCode, LocalDateTime requestedAt) {
            this.newEmail = newEmail;
            this.verificationCode = verificationCode;
            this.requestedAt = requestedAt;
        }

        public String getNewEmail() {
            return newEmail;
        }
        public String getVerificationCode() {
            return verificationCode;
        }
        public LocalDateTime getRequestedAt() {
            return requestedAt;
        }
    }
}
