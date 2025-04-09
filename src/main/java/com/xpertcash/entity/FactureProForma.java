package com.xpertcash.entity;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactureProForma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String numeroFacture;
    private LocalDate dateCreation;
    private String description;

    @Enumerated(EnumType.STRING)
    private StatutFactureProForma statut = StatutFactureProForma.BROUILLON;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "client_id")
    private Client client;



    @OneToMany(mappedBy = "factureProForma", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<LigneFactureProforma> lignesFacture;
    
    
    

}
