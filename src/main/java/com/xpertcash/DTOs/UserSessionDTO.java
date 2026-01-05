package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSessionDTO {
    private Long id;
    private String deviceId;
    private String deviceName;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    private LocalDateTime expiresAt;
    private boolean isActive;
    private boolean isCurrentSession; // Indique si c'est la session courante
}


