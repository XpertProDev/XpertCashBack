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

    private Long id;
    private String nom;
    private Boolean active;
    private LocalDateTime createdAt;
    private String pays;
    private String secteur;
    private String adminId;
    private String adminNom;
    private String adminPhone;
    private String adminEmail;
    private long nombreUtilisateursEntreprise;
    private LocalDateTime derniereConnexion;
}


