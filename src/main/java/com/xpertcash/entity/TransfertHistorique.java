package com.xpertcash.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class TransfertHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "magasin_id", nullable = false)
    private Magasin magasin; // Le magasin d'où le produit est transféré

    @ManyToOne
    @JoinColumn(name = "boutique_id", nullable = false)
    private Boutique boutique; // La boutique où le produit est transféré

    @ManyToOne
    @JoinColumn(name = "produit_id", nullable = false)
    private Produits produit; // Le produit transféré

    @Column(nullable = false)
    private int ancienneQuantite; // Quantité avant transfert

    @Column(nullable = false)
    private int nouvelleQuantite; // Quantité après transfert

    @Column(nullable = false)
    private int quantite; // Quantité transférée

    @Column(nullable = false)
    private LocalDateTime dateTransfert = LocalDateTime.now(); // Date du transfert

    // Vous pouvez ajouter d'autres champs comme des informations supplémentaires sur l'utilisateur, par exemple:
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Utilisateur ayant effectué le transfert (facultatif)
}

