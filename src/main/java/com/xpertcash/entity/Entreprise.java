package com.xpertcash.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
   private String suffixe;
   @Column(nullable = true)
   private String prefixe;
    
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

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = true)  
    private User admin;

    @OneToMany(mappedBy = "entreprise", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"entreprise"})
    private List<FactureProForma> facturesProforma = new ArrayList<>();



    // Générer un identifiant unique
    public static String generateIdentifiantEntreprise() {
        return "Xpc" + String.format("%04d", new Random().nextInt(10000));
    }

}
