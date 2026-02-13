package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetteItemDTO {

    private Long id;
    private String type;
    // private String source;
    private Double montantInitial; // Montant de départ de la dette
    private Double montantRestant;
    private LocalDateTime date;
    private String description;
    private String numero;
    // Pour FACTURE_IMPAYEE et VENTE_CREDIT : client / contact correspondent au client
    // Pour DEPENSE_DETTE : client / contact correspondent au fournisseur
    private String client;
    private String contact;
    // Pour ENTREE_DETTE : responsable et son contact
    private String responsable;
    private String responsableContact;

    /** Pour VENTE_CREDIT (dettes POS) : vendeur et boutique (tri / filtre). */
    private Long vendeurId;
    private String vendeurNom;
    private Long boutiqueId;
    private String boutiqueNom;

    /** Pour VENTE_CREDIT (dettes POS) : remise globale appliquée à la vente (utilisée uniquement si typeRemise = GLOBALE). */
    private Double remiseGlobale;

    private String typeRemise;

    /** Pour VENTE_CREDIT (dettes POS) : liste des produits vendus (quel produit, quelle quantité, quel montant, remise). */
    private List<LigneProduitDetteDTO> produits;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LigneProduitDetteDTO {
        private Long produitId;
        private String nomProduit;
        private Integer quantite;
        private Double prixUnitaire;
        private Double remise;       // Remise sur la ligne
        private Double montantLigne;
    }
}


