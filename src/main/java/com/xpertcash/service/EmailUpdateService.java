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

    // Map pour stocker temporairement les demandes de modification d'email (userId -> PendingEmailUpdate)
    private Map<Long, PendingEmailUpdate> pendingEmailUpdates = new ConcurrentHashMap<>();

    /**
     * Demande de mise à jour d'email.
     * Génère un code de vérification et l'envoie à l'email actuel de l'utilisateur.
     */
    public void requestEmailUpdate(Long userId, EmailUpdateRequest request) {
        // Récupérer l'utilisateur
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // Vérifier que le nouvel email est différent de l'email actuel
        if (user.getEmail().equalsIgnoreCase(request.getNewEmail())) {
            throw new RuntimeException("Vous utilisez déjà cet email.");
        }

        // Vérifier que le nouvel email n'est pas déjà utilisé par un autre utilisateur (isolé par entreprise)
        Long entrepriseId = user.getEntreprise() != null ? user.getEntreprise().getId() : null;
        if (entrepriseId == null) {
            throw new RuntimeException("L'utilisateur n'a pas d'entreprise associée.");
        }
        if (usersRepository.findByEmailAndEntrepriseId(request.getNewEmail(), entrepriseId).isPresent()) {
            throw new RuntimeException("Cet email est déjà utilisé par un autre utilisateur dans votre entreprise.");
        }

        // Générer un code de vérification à 6 chiffres
        String verificationCode = String.format("%06d", new Random().nextInt(1000000));

        // Enregistrer la demande en attente
        PendingEmailUpdate pendingUpdate = new PendingEmailUpdate(request.getNewEmail(), verificationCode, LocalDateTime.now());
        pendingEmailUpdates.put(userId, pendingUpdate);

                try {
            // Envoyer le code de vérification à l'email actuel de l'utilisateur
            mailService.sendEmailVerificationCode(user.getEmail(), verificationCode);
        } catch (MessagingException e) {  // Capturer MessagingException ici
            System.err.println("Erreur lors de l'envoi du code de vérification à l'email : " + e.getMessage());
            throw new RuntimeException("Erreur lors de l'envoi du code de vérification. L'email n'a pas pu être envoyé.");
        }

    }

    /**
     * Confirmation de la mise à jour d'email.
     * Vérifie le code et met à jour l'email de l'utilisateur.
     */
    public void confirmEmailUpdate(Long userId, EmailUpdateConfirmationRequest confirmationRequest) {
        PendingEmailUpdate pendingUpdate = pendingEmailUpdates.get(userId);
        if (pendingUpdate == null) {
            throw new RuntimeException("Aucune demande de mise à jour d'email n'a été trouvée pour cet utilisateur.");
        }

        // Ici, vous pouvez ajouter une vérification de l'expiration de la demande en fonction de pendingUpdate.requestedAt

        // Vérifier que le code de vérification correspond
        if (!pendingUpdate.getVerificationCode().equals(confirmationRequest.getVerificationCode())) {
            throw new RuntimeException("Code de vérification invalide.");
        }

        // Mettre à jour l'email de l'utilisateur
        User user = usersRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        user.setEmail(pendingUpdate.getNewEmail());
        usersRepository.save(user);

        // Supprimer la demande en attente après confirmation
        pendingEmailUpdates.remove(userId);
    }

    // Classe interne pour stocker les demandes de modification d'email en attente
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
