package com.xpertcash.DTOs.PASSWORD;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordUpdateRequest {

    @NotBlank(message = "Le mot de passe actuel ne peut pas être vide.")
    private String currentPassword;

    @NotBlank(message = "Le nouveau mot de passe ne peut pas être vide.")
    @Size(min = 8, message = "Le nouveau mot de passe doit contenir au moins 8 caractères.")
    private String newPassword;
}
