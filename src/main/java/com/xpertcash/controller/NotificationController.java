package com.xpertcash.controller;

import com.xpertcash.entity.Notification;
import com.xpertcash.entity.User;
import com.xpertcash.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Correction ici
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class NotificationController { // Renommer en NotificationController

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/notifications/unread")
    public List<Notification> getUnreadNotifications(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return notificationRepository.findByUserIdAndReadFalse(user.getId());
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        notificationRepository.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}