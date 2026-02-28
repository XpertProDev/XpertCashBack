package com.xpertcash.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.xpertcash.entity.Module.AppModule;

@Entity
@Data
public class Entreprise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nomEntreprise;


    @Column(unique = true, nullable = false)
    private String identifiantEntreprise;

    private String telephone;
    private String pays;
    private String secteur;   

    @Column(nullable = true)
    private String email;

    @Column(nullable = true)
    private String siteWeb;

    @Column(nullable = true)
    private String siege;

    @Column(nullable = true)
    private String signataire;

    @Column(nullable = true)
    private String signataireNom;

     @Column(nullable = true)
    private String signaturNum;

    @Column(nullable = true)
     private String cachetNum;

    @Column(nullable = true)
    private String prefixe;

    @Column(nullable = true)
    private String suffixe;

    @Column(nullable = true)
    private Double tauxTva;

    @Column(nullable = true)
    private String nina;

    @Column(nullable = true)
    private String rccm;
    
    @Column(nullable = true)
    private String nif;
    
    @Column(nullable = true)
    private String banque;


    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> utilisateurs = new ArrayList<>();

    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Boutique> boutiques = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private String logo;

    @Column(nullable = true)
    private String adresse;

    @Column(nullable = true)
    private Boolean active = true;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = true)  
    private User admin;

    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<FactureProForma> facturesProforma = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    private Set<AppModule> modulesActifs = new HashSet<>();

    @Column(name = "date_fin_essai_modules_payants")
    private LocalDateTime dateFinEssaiModulesPayants;

    /** Quota d'utilisateurs pour cette entreprise (admin inclus). Le Super Admin peut l'augmenter. */
    @Column(name = "max_utilisateurs", nullable = true)
    private Integer maxUtilisateurs = DEFAULT_MAX_UTILISATEURS;

    /** Quota par défaut pour un nouveau compte (admin + 1 utilisateur). Source unique pour toute l'app. */
    public static final int DEFAULT_MAX_UTILISATEURS = 2;

    /** Retourne le quota effectif (jamais null pour la logique métier). */
    public int getMaxUtilisateursOrDefault() {
        return maxUtilisateurs != null ? maxUtilisateurs : DEFAULT_MAX_UTILISATEURS;
    }

    // Générer un identifiant unique
    public static String generateIdentifiantEntreprise() {
        return "Xpc" + String.format("%04d", new Random().nextInt(10000));
    }

}
