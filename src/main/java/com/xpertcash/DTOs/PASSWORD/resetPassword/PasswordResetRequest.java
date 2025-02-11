package com.xpertcash.DTOs.PASSWORD.resetPassword;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class PasswordResetRequest {

    @NotBlank(message = "L'email ne peut pas être vide.")
    @Email(message = "L'email doit être valide.")
    private String email;
}
