package com.xpertcash.controller;

import com.xpertcash.DTOs.GlobalNotificationDto;
import com.xpertcash.configuration.JwtUtil;
import com.xpertcash.entity.GlobalNotification;
import com.xpertcash.entity.User;
import com.xpertcash.service.GlobalNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class GlobalNotificationController {

    private final GlobalNotificationService globalNotificationService;
    private final JwtUtil jwtUtil;

    public GlobalNotificationController(GlobalNotificationService globalNotificationService,
                                        JwtUtil jwtUtil) {
        this.globalNotificationService = globalNotificationService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Récupère toutes les notifications pour l'utilisateur authentifié,
     * triées par date décroissante.
     */
    @GetMapping("/list/global/notifications")
    public ResponseEntity<List<GlobalNotificationDto>> getUserNotifications(
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        // 1️⃣ Vérifier le header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2️⃣ Extraire et valider le token
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 3️⃣ Récupérer les notifications
        List<GlobalNotification> notifs = globalNotificationService.getUserNotifications(userId);

        // 4️⃣ Mapper vers le DTO
        // 4️⃣ Mapper vers le DTO
        List<GlobalNotificationDto> dtoList = notifs.stream()
                .map(notif -> new GlobalNotificationDto(
                        notif.getId(),
                        notif.getMessage(),
                        notif.getCreatedAt(),
                        notif.getRecipient().getNomComplet(),
                        notif.isRead() // AJOUTEZ CE CHAMP
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    /**
     * (Optionnel) Exemple de point pour marquer une notification comme lue.
     */
    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader
    ) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String token = authHeader.substring(7);
        Long userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        globalNotificationService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }
}
