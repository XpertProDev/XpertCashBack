package com.xpertcash.DTOs.PASSWORD.resetPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetConfirmation {

    @NotBlank(message = "L'email ne peut pas être vide.")
    @Email(message = "L'email doit être valide.")
    private String email;

    @NotBlank(message = "Le code de vérification ne peut pas être vide.")
    private String verificationCode;

    @NotBlank(message = "Le nouveau mot de passe ne peut pas être vide.")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères.")
    private String newPassword;
}
