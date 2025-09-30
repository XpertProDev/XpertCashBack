package com.xpertcash.entity.PROSPECT;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Enum.PROSPECT.ProspectType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Prospect {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ProspectType type;

    // Champs pour ENTREPRISE
    private String nom;
    private String secteur;


    // Champs pour PARTICULIER
    private String nomComplet;
    private String profession;




    // Champs communs
    private String ville;
    private String adresse;
    private String pays;
    private String email;
    private String telephone;
    private String notes; // notes générales sur le prospect

    // Statut de conversion
    private Boolean convertedToClient = false;
    private LocalDateTime convertedAt;
    private Long clientId; // ID du client créé (Client ou EntrepriseClient)
    private String clientType; // "CLIENT" ou "ENTREPRISE_CLIENT"

    @OneToMany(mappedBy = "prospect", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Interaction> interactions = new ArrayList<>();

    @OneToMany(mappedBy = "prospect", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProspectAchat> achats = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    private LocalDateTime createdAt = LocalDateTime.now();

}
