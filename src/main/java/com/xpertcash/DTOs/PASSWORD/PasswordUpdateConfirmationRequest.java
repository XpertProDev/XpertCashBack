package com.xpertcash.DTOs.PASSWORD;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordUpdateConfirmationRequest {

    @NotBlank(message = "Le code de vérification ne peut pas être vide.")
    private String verificationCode;
}
