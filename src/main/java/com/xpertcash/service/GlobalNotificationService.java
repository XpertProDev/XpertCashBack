package com.xpertcash.service;

import com.xpertcash.entity.GlobalNotification;
import com.xpertcash.entity.User;
import com.xpertcash.repository.GlobalNotificationRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GlobalNotificationService {

    private final GlobalNotificationRepository notificationRepo;

    public GlobalNotificationService(GlobalNotificationRepository notificationRepo) {
        this.notificationRepo = notificationRepo;
    }

    @Transactional
    public void notifyRecipients(List<User> recipients, String message) {
        recipients.forEach(user -> {
            GlobalNotification notification = new GlobalNotification(user, message);
            notificationRepo.save(notification);
        });
    }

    @Transactional
    public void notifySingle(User recipient, String message) {
        notificationRepo.save(new GlobalNotification(recipient, message));
    }

    public List<GlobalNotification> getUserNotifications(Long userId) {
        return notificationRepo.findByRecipientIdOrderByCreatedAtDesc(userId);
    }
}
