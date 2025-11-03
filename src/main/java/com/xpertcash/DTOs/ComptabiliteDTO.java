package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComptabiliteDTO {
    private ChiffreAffairesDTO chiffreAffaires;
    private VentesDTO ventes;
    private FacturationDTO facturation;
    private DepensesDTO depenses;
    private List<BoutiqueInfoDTO> boutiques;
    private List<BoutiqueDisponibleDTO> boutiquesDisponibles;
    private ClientsDTO clients;
    private VendeursDTO vendeurs;
    private ActivitesDTO activites;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChiffreAffairesDTO {
        private Double total;
        private Double duJour;
        private Double duMois;
        private Double deLAnnee;
        private Double totalVentes;
        private Double totalFactures;
        private Double totalPaiementsFactures;
        private List<VenteCADetail> ventesDetails;
        private List<FactureDetail> factureDetails;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VenteCADetail {
        private Long venteId;
        private String numeroTicket;
        private java.time.LocalDateTime dateHeure;
        private String modePaiement;
        private Double remiseGlobale;
        private Double montantNet;
        private String statut;
    }

    // Note: PaiementCADetail supprimé au profit de FactureDetail plus lisible

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VentesDTO {
        private Integer nombreTotal;
        private Double montantTotal;
        private Integer duJour;
        private Double montantDuJour;
        private Integer duMois;
        private Double montantDuMois;
        private Integer deLAnnee;
        private Double montantDeLAnnee;
        private Integer annulees; // ventes totalement remboursées
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FacturationDTO {
        private Integer nombreTotalFactures;
        private Double montantTotalFactures;
        private Double montantPaye;
        private Double montantImpaye;
        private Integer duJour;
        private Double montantDuJour;
        private Integer duMois;
        private Double montantDuMois;
        private Integer deLAnnee;
        private Double montantDeLAnnee;
        private List<FactureDetail> details;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FactureDetail {
        private Long factureId;
        private String numeroFacture;
        private java.time.LocalDate dateCreation;
        private Double totalHT;
        private Double remise;
        private Boolean tva;
        private Double totalFacture;
        private Double montantPaye;
        private Double montantRestant;
        private String statutPaiement;
        private String encaissePar;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DepensesDTO {
        private Integer nombreTotal;
        private Double montantTotal;
        private Integer duJour;
        private Double montantDuJour;
        private Integer duMois;
        private Double montantDuMois;
        private Integer deLAnnee;
        private Double montantDeLAnnee;
        private List<DepenseDetail> details;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DepenseDetail {
        private java.time.LocalDateTime date;
        private String libelle;
        private String methode;
        private Double montant;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BoutiqueInfoDTO {
        private Long id;
        private String nom;
        private Double chiffreAffaires;
        private Integer nombreVentes;
        private Double totalDepenses;
        private Integer nombreDepenses;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BoutiqueDisponibleDTO {
        private Long id;
        private String nom;
        private String type;
        private String email;
        private String adresse;
        private String telephone;
        private java.time.LocalDateTime dateCreation;
        private String statut;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ClientsDTO {
        private Integer nombreTotal;
        private Integer actifs; // Clients ayant au moins une vente
        private Double montantTotalAchete;
        private List<MeilleurClientDTO> meilleursClients; // Top 3
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MeilleurClientDTO {
        private Long id;
        private String nomComplet;
        private String email;
        private String telephone;
        private Double montantAchete;
        private Integer nombreAchats;
        private String type;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VendeursDTO {
        private Integer nombreTotal;
        private Integer actifs; // Vendeurs ayant au moins une vente
        private Double chiffreAffairesTotal;
        private List<MeilleurVendeurDTO> meilleursVendeurs; // Top 3
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MeilleurVendeurDTO {
        private Long id;
        private String nomComplet;
        private String email;
        private Double chiffreAffaires;
        private Integer nombreVentes;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ActivitesDTO {
        private Integer nombreVentesTotal;
        private Integer nombreFacturesTotal;
        private Integer nombreDepensesTotal;
        private Integer nombreTransactionsJour;
    }
}

