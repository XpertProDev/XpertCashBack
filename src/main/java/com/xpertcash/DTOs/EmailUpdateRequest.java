package com.xpertcash.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailUpdateRequest {

    @NotBlank(message = "Le nouvel email ne peut être vide.")
    @Email(message = "Le nouvel email doit être valide.")
    private String newEmail;
}
