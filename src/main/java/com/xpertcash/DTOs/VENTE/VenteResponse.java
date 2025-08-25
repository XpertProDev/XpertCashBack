package com.xpertcash.DTOs.VENTE;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

import com.xpertcash.entity.Caisse;

@Data
public class VenteResponse {
    private Long venteId;
    private Caisse caisse;
    private Long boutiqueId;
    private Long vendeurId;
    private LocalDateTime dateVente;
    private Double montantTotal;
    private String description;
    private String nomVendeur;
    private String nomBoutique;
    private String clientNom;
    private String clientNumero;
    private String modePaiement;
    private Double montantPaye;
    private List<LigneVenteDTO> lignes;
    private Double remiseGlobale;
    private String numeroFacture;



    @Data
    public static class LigneVenteDTO {
        private Long produitId;
        private String nomProduit;
        private Integer quantite;
        private Double prixUnitaire;
        private Double montantLigne;
        private Double remise;

    }
}