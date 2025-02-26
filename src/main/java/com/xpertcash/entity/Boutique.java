package com.xpertcash.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Boutique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomBoutique;

    @ManyToOne
    @JoinColumn(name = "entreprise_id", nullable = false)
    @JsonBackReference
    private Entreprise entreprise;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user; // L'utilisateur qui possède la boutique

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "boutique", cascade = CascadeType.ALL)
    @JsonBackReference
    private List<ProduitBoutique> produitsBoutique = new ArrayList<>();


}
