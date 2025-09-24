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
    private String sector;
    private String address;
    private String city;
    private String country;

    // Champs pour PARTICULIER
    private String nomComplet;
    private String adresse;
    private String pays;



    // Champs communs
    private String email;
    private String telephone;
    private String notes; // notes générales sur le prospect

    @OneToMany(mappedBy = "prospect", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Interaction> interactions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    private LocalDateTime createdAt = LocalDateTime.now();

}
