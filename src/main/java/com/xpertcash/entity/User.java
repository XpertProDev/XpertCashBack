package com.xpertcash.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String uuid;

    @Column(nullable = false)
    private String personalCode;
    
    @Size(min = 2, message = "champs nom trop courte")

    @Column(nullable = false)
    private String nomComplet;

    @NotNull(message = "Champs vide")


    @Column(unique = true, nullable = false)
    private String email;

    @NotNull(message = "Champs vide")


    @Column(nullable = false)
     @JsonIgnore
    private String password;

    @NotNull(message = "Champs vide")
    @Column(unique = true ,nullable = false)
    private String phone;


    @Column(nullable = false)
    private String pays;

    @Column(nullable = true)
    private String photo;

    @NotNull(message = "Date de creation de Users")
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "qr_code_url")
    private String qrCodeUrl;

    private String activationCode;

    private boolean activatedLien = false;
    private boolean enabledLien = true;

    private LocalDateTime lastActivity;
    private boolean locked = false;


    // LES RELATIONS

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @ManyToOne
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // @ManyToOne
    // @JoinColumn(name = "boutique_id")
    // private Boutique boutique;

   @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserBoutique> userBoutiques = new ArrayList<>();

}
