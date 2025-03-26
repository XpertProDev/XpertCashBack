package com.xpertcash.DTOs;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xpertcash.entity.Facture;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FactureDTO {
    private Long id;
    private String numeroFacture;
   @JsonInclude(JsonInclude.Include.NON_NULL)
    private String codeFournisseur; 
    private String type;
    private String description;
    private LocalDateTime dateFacture;
    private String nomUtilisateur;
    private String telephoneUtilisateur;
    private List<ProduitFactureDTO> produits;
    private Long boutiqueId;

    public FactureDTO(Facture facture) {
        this.id = facture.getId();
        this.numeroFacture = facture.getNumeroFacture();
        this.codeFournisseur = facture.getCodeFournisseur();
        this.type = facture.getType();
        this.description = facture.getDescription();
        this.dateFacture = facture.getDateFacture();
        

        if (facture.getUser() != null) {
            this.nomUtilisateur = facture.getUser().getNomComplet();
            this.telephoneUtilisateur = facture.getUser().getPhone();
        }

        if (facture.getBoutique() != null) {
            this.boutiqueId = facture.getBoutique().getId();
        }

        // Convertir les FactureProduit en ProduitFactureDTO
        this.produits = facture.getFactureProduits().stream()
            .map(fp -> new ProduitFactureDTO(fp.getProduit(), fp.getQuantite()))
            .collect(Collectors.toList());
    }

}
