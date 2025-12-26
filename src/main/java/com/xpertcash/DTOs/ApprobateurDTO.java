package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO optimisé pour représenter un approbateur avec seulement les informations essentielles
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApprobateurDTO {
    private String uuid;
    private String nomComplet;
}

