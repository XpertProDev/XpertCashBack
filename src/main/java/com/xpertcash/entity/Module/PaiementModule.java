package com.xpertcash.entity.Module;

import jakarta.persistence.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.xpertcash.entity.Entreprise;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Data
@Getter
@Setter

public class PaiementModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private BigDecimal montant;
    private String devise = "XOF";
    private String nomCompletProprietaire;
    private String emailProprietaireCarte;
    private String pays;
    private String adresse;
    private String ville;
    private LocalDateTime datePaiement;

    @ManyToOne
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @ManyToOne
    @JoinColumn(name = "module_id")
    private AppModule module;

    private String referenceTransaction;  // Utile si tu intègres un vrai paiement plus tard

    // Getters, setters, constructeur vide requis par JPA
}