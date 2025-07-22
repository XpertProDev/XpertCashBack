package com.xpertcash.DTOs;

import com.xpertcash.entity.GlobalNotification;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GlobalNotificationDto {
    private Long id;
    private String message;
    private LocalDateTime createdAt;
    private String senderName; // optionnel, selon ce que vous voulez afficher
    // Ajoutez le getter
    @Getter
    private boolean read;

    public GlobalNotificationDto(GlobalNotification notif) {
        this.id = notif.getId();
        this.message = notif.getMessage();
        this.createdAt = notif.getCreatedAt();
        // si vous voulez afficher qui a déclenché la notif, ajoutez dans l'entité GlobalNotification
        // un champ comme `createdBy` ou prenez depuis notif.getRecipient() si c'est vous-même
        this.senderName = notif.getRecipient().getNomComplet();
        this.read = read; // Initialisez le champ
    }
}
