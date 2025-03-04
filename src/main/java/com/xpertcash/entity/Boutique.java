package com.xpertcash.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Boutique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomBoutique;
    private String adresse;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    @ManyToOne
    @JoinColumn(name = "entreprise_id", nullable = false)
    @JsonBackReference
    private Entreprise entreprise;

    @OneToMany(mappedBy = "boutique", cascade = CascadeType.ALL)
    @JsonBackReference
    private List<Stock> stocks = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

}
