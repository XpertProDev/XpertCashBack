package com.xpertcash.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entité pour gérer les sessions utilisateur par appareil
 * Permet à un utilisateur de se connecter sur plusieurs appareils simultanément
 */
@Entity
@Getter
@Setter
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_user_uuid", columnList = "user_uuid"),
    @Index(name = "idx_session_token", columnList = "session_token"),
    @Index(name = "idx_device_id", columnList = "device_id")
})
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_uuid", nullable = false)
    private String userUuid;

    @Column(name = "session_token", unique = true, nullable = true, length = 500)
    private String sessionToken; // JWT token complet (nullable pour permettre création puis mise à jour)

    @Column(name = "device_id", nullable = true)
    private String deviceId; // Identifiant de l'appareil (optionnel, généré si absent)

    @Column(name = "device_name", nullable = true)
    private String deviceName; // Nom de l'appareil (ex: "iPhone 12", "Chrome sur Windows")

    @Column(name = "ip_address", nullable = true)
    private String ipAddress; // Adresse IP de connexion

    @Column(name = "user_agent", nullable = true, length = 500)
    private String userAgent; // User-Agent du navigateur/appareil

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_activity", nullable = false)
    private LocalDateTime lastActivity;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // Session active ou révoquée

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Constructeur par défaut
    public UserSession() {
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    // Méthode pour mettre à jour la dernière activité
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    // Méthode pour vérifier si la session est expirée
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) || !isActive;
    }
}


