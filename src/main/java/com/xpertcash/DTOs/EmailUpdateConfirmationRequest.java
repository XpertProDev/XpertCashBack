package com.xpertcash.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailUpdateConfirmationRequest {

    @NotBlank(message = "Le code de vérification ne peut être vide.")
    private String verificationCode;
}
