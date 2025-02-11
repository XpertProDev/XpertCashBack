package com.xpertcash.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateEntrepriseRequest {
    @NotBlank(message = "Le nom de l'entreprise ne peut pas être vide.")
    @Size(min = 3, message = "Le nom de l'entreprise doit contenir au moins 3 caractères.")
    private String nomEntreprise;
}
