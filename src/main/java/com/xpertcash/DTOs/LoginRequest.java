package com.xpertcash.DTOs;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
    // Paramètres optionnels pour la gestion des sessions multiples
    private String deviceId;      // Identifiant de l'appareil (optionnel, généré si absent)
    private String deviceName;    // Nom de l'appareil (ex: "iPhone 12", "Chrome sur Windows")
}
