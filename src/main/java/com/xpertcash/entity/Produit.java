package com.xpertcash.entity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.xpertcash.entity.Enum.TypeProduit;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(
    uniqueConstraints = @UniqueConstraint(columnNames = {"codeGenerique", "boutique_id"})
)
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private Double prixVente;
    private Double prixAchat;
    private Integer quantite;
    private Integer seuilAlert;
    private String description;
    @Column(nullable = false)
    private String codeGenerique;
    private String codeBare;
    private String photo;
    private Boolean enStock = false;

    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private Boolean deleted = false;
    private LocalDateTime deletedAt;

    @Column(name = "date_preemption")
    private LocalDate datePreemption;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Enumerated(EnumType.STRING)
    private TypeProduit typeProduit;



    @ManyToOne 
    @JoinColumn(name = "boutique_id", nullable = false)
    @JsonBackReference("produit-boutique")
    private Boutique boutique; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Categorie categorie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unite_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Unite uniteDeMesure;

    @OneToMany(fetch = FetchType.LAZY)
   // @JoinColumn(name = "stocks_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<Stock> stocks;

    @OneToMany(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JoinColumn(name = "factureProduits_id", nullable = true)
    private List<FactureProduit> factureProduits = new ArrayList<>();

    public Boutique getBoutique() {
        return boutique;
    }

    public void setBoutique(Boutique boutique) {
        this.boutique = boutique;
    }

}
