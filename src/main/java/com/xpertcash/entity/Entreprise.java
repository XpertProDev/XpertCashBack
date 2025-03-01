package com.xpertcash.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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


    // Générer un identifiant unique
    public static String generateIdentifiantEntreprise() {
        return "Xpc" + String.format("%04d", new Random().nextInt(10000));
    }

}
