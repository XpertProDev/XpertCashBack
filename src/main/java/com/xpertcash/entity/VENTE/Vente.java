package com.xpertcash.entity.VENTE;

import lombok.Data;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.CascadeType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.EnumType;

import java.time.LocalDateTime;
import java.util.List;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Caisse;
import com.xpertcash.entity.Client;
import com.xpertcash.entity.EntrepriseClient;
import com.xpertcash.entity.ModePaiement;
import com.xpertcash.entity.User;

@Data
@Entity
public class Vente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "boutique_id")
    private Boutique boutique;

    @ManyToOne
    @JoinColumn(name = "vendeur_id")
    private User vendeur;

    private LocalDateTime dateVente;

    private Double montantTotal;

    private String description;

    private String clientNom;
    private String clientNumero;

    private Double remiseGlobale;

    


    @Enumerated(EnumType.STRING)
    private ModePaiement modePaiement;
    private Double montantPaye;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL)
    private List<VenteProduit> produits;

    @Enumerated(EnumType.STRING)
    private VenteStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caisse_id")
    private Caisse caisse;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne
    @JoinColumn(name = "entreprise_client_id")
    private EntrepriseClient entrepriseClient;


}