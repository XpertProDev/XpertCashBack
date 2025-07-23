package com.xpertcash.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type;
    private String description;
    private LocalDateTime dateFacture;
    private String numeroFacture;
    private String codeFournisseur;

    @ManyToOne
    @JoinColumn(name = "boutique_id", referencedColumnName = "id", nullable = false)
    private Boutique boutique;

    @ManyToOne(optional = true)
    @JoinColumn(name = "fournisseur_id", nullable = true)
    private Fournisseur fournisseur;

    



    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FactureProduit> factureProduits = new ArrayList<>();

    public Boutique getBoutique() {
        return boutique;
    }

    public void setBoutique(Boutique boutique) {
        this.boutique = boutique;
    }
}
