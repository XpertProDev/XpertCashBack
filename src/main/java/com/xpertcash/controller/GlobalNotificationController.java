package com.xpertcash.controller;

import com.xpertcash.DTOs.GlobalNotificationDto;
import com.xpertcash.service.AuthenticationHelper;
import com.xpertcash.entity.GlobalNotification;
import com.xpertcash.entity.User;
import com.xpertcash.service.GlobalNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class GlobalNotificationController {

    private final GlobalNotificationService globalNotificationService;
    private final AuthenticationHelper authHelper;

    public GlobalNotificationController(GlobalNotificationService globalNotificationService,
                                        AuthenticationHelper authHelper) {
        this.globalNotificationService = globalNotificationService;
        this.authHelper = authHelper;
    }

    /**
     * Récupère toutes les notifications pour l'utilisateur authentifié,
     * triées par date décroissante.
     */
    @GetMapping("/list/global/notifications")
    public ResponseEntity<List<GlobalNotificationDto>> getUserNotifications(
            HttpServletRequest request
    ) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            
            // 3️⃣ Récupérer les notifications
            List<GlobalNotification> notifs = globalNotificationService.getUserNotifications(user.getId());

            // 4️⃣ Mapper vers le DTO
            List<GlobalNotificationDto> dtoList = notifs.stream()
                    .map(notif -> new GlobalNotificationDto(
                            notif.getId(),
                            notif.getMessage(),
                            notif.getCreatedAt(),
                            notif.getRecipient().getNomComplet(),
                            notif.isRead()
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * (Optionnel) Exemple de point pour marquer une notification comme lue.
     */
    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        try {
            User user = authHelper.getAuthenticatedUserWithFallback(request);
            globalNotificationService.markAsRead(id, user.getId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
