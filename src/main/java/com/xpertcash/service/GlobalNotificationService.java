package com.xpertcash.service;

import com.xpertcash.DTOs.GlobalNotificationDto;
import com.xpertcash.entity.GlobalNotification;
import com.xpertcash.entity.User;
import com.xpertcash.repository.GlobalNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GlobalNotificationService {

    private final GlobalNotificationRepository notificationRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private static final Logger log = LoggerFactory.getLogger(GlobalNotificationService.class);

    public GlobalNotificationService(GlobalNotificationRepository notificationRepo,
                                     SimpMessagingTemplate messagingTemplate) {
        this.notificationRepo = notificationRepo;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void notifyRecipients(List<User> recipients, String message) {
        recipients.forEach(user -> {
            log.info("Envoi notification à: {} | Message: {}", user.getEmail(), message);
            GlobalNotification notif = new GlobalNotification(user, message);
            notificationRepo.save(notif);
            messagingTemplate.convertAndSendToUser(
                    user.getId().toString(),
                    "/queue/notifications",
                    new GlobalNotificationDto(notif)
            );
        });
    }

    @Transactional
    public void notifySingle(User recipient, String message) {
        GlobalNotification notif = new GlobalNotification(recipient, message);
        notificationRepo.save(notif);
        messagingTemplate.convertAndSendToUser(
                recipient.getId().toString(),
                "/queue/notifications",
                new GlobalNotificationDto(notif)
        );
    }


    /**
     * Récupère toutes les notifications pour un utilisateur, les plus récentes d'abord.
     */
    public List<GlobalNotification> getUserNotifications(Long userId) {
        return notificationRepo.findByRecipientIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        GlobalNotification notif = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification introuvable : " + notificationId));

        if (!notif.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Accès refusé : cette notification n'appartient pas à l'utilisateur.");
        }

        notif.setRead(true);
        notificationRepo.save(notif);
    }

}