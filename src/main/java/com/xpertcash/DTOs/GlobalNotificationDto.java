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
    private String senderName;
    
    @Getter
    private boolean read;

    public GlobalNotificationDto(GlobalNotification notif) {
        this.id = notif.getId();
        this.message = notif.getMessage();
        this.createdAt = notif.getCreatedAt();
        this.senderName = notif.getRecipient().getNomComplet();
        this.read = read;
    }
}