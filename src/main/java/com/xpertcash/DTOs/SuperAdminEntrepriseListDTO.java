package com.xpertcash.DTOs;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO minimal pour la vue liste des entreprises du SUPER_ADMIN.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SuperAdminEntrepriseListDTO {

    private Long id;                 // numéro de l'entreprise
    private String nom;              // nom de l'entreprise
    private LocalDateTime createdAt; // date de création
    private String pays;             // pays
    private String secteur;          // secteur
    private String adminNom;         // nom complet de l'admin
    private String adminPhone;       // téléphone de l'admin
    private String adminEmail;       // email de l'admin
    private long nombreUtilisateursEntreprise; // nombre de personnes dans l'entreprise
}


