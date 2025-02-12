package com.xpertcash.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //=========================== POUR NOM COMPLET ===========================
    // Not nul de Spring boot
    //@NotNull(message = "Champs vide")

    @Size(min = 2, message = "champs nom trop courte")

    // Not null de basse de donnée
    @Column(nullable = false)
    private String nomComplet;

    //=========================== POUR NOM e/prs ===========================
    /* Not nul de Spring boot
    @NotNull(message = "Champs vide")

    @Size(min = 2, message = "champs nom trop courte")

    // Not null de basse de donnée
    @Column(nullable = false)
    private String nomEntreprise;*/

    //=========================== POUR EMAIL ===========================
    // Not nul de Spring boot
    @NotNull(message = "Champs vide")

    @Email(message = "email incorrect veille saisir correctement ex : example@gmail.com")
    // Not Null de base de donnée
    @Column(unique = true, nullable = false)
    private String email;

    //======================= POUR MOT DE PASSE ===============================
    // Not Null de Spring
    @NotNull(message = "Champs vide")

    // size : lenght de notre Mot de passe
    // message : message afficher si le champs de notre Mot de passe est vide
    @Size(min = 7, message = "Saisissez un Mot de pass correct")

    // Not Null de base de donnée
    @Column(nullable = false)
    private String password;

    //=========================== POUR PHONE ===========================
    // Not nul de Spring boot
    @NotNull(message = "Champs vide")

    @Size(min = 8, message = "champs phone doit être 8 chiffres")

    // Not null de basse de donnée
    @Column(unique = true ,nullable = false)
    private String phone;

    //=========================== POUR PHOTO ===========================
    @Column(nullable = true)
    private String photo;

    //=========================== POUR DATE & HEURE DE CREATION ===========================
    // Not nul de Spring boot
    @NotNull(message = "Date de creation de Users")
    // Not null de basse de donnée
    @Column(nullable = false)
    private LocalDateTime createdAt;

    //=========================== POUR CODE D'ACTIVATION & VÉRROUILLER ===========================
    // --- Activation et PIN ---
    // Code PIN à 4 chiffres (utilisé pour activer ou déverrouiller)
    private String activationCode;

    //=========================== POUR CODE D'ACTIVATION AVEC UN LIEN ===========================
    private boolean activatedLien = false; // Passe à true si le compte est activé via le lien
    private boolean enabledLien = true;    // Si false, l'accès est refusé (ex. après 24h sans activation)

    //=========================== POUR CODE D'ACTIVATION & VÉRROUILLER ===========================
    // --- Suivi d’activité / verrouillage ---
    private LocalDateTime lastActivity; // Dernière activité (ex. lors de la connexion)
    private boolean locked = false;


    // LES RELATIONS

    @ManyToOne
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

}
