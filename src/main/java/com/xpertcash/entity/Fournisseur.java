package com.xpertcash.entity;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(indexes = {
        @Index(name = "idx_fournisseur_entreprise_id", columnList = "entreprise_id"),
        @Index(name = "idx_fournisseur_nom_complet", columnList = "nomComplet"),
        @Index(name = "idx_fournisseur_nom_societe", columnList = "nomSociete"),
        @Index(name = "idx_fournisseur_email", columnList = "email"),
        @Index(name = "idx_fournisseur_telephone", columnList = "telephone")
})
@Data 
@AllArgsConstructor
@NoArgsConstructor
public class Fournisseur {

        @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nomComplet;
    private String nomSociete;
    private String description;
    private String pays;
    private String telephone; 
    private String email;
    private String ville;
    private String adresse;
    private LocalDateTime createdAt;
    private String photo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entreprise_id")
    @JsonIgnoreProperties({"facturesProforma", "identifiantEntreprise", "utilisateurs", "adresse", "boutiques", "createdAt", "logo", "admin"})
    private Entreprise entreprise;

   


}
