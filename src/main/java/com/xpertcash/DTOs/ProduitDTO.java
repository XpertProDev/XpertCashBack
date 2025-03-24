package com.xpertcash.DTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class ProduitDTO {
    private Long id;

    @NotNull(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String nom;

    @NotNull(message = "Le prix de vente est obligatoire")
    //@Positive(message = "Le prix de vente doit être positif")
    private Double prixVente;

    @NotNull(message = "Le prix d'achat est obligatoire")
    //@Positive(message = "Le prix d'achat doit être positif")
    private Double prixAchat;

//    @NotNull(message = "La quantité est obligatoire")
    //@Positive(message = "La quantité doit être positive")
    private Integer quantite;

    private Integer seuilAlert;
    private Long categorieId;
    private Long uniteId;
    private String codeBare;
    private String codeGenerique;
    private String description;
    private String photo;
    private Boolean enStock;
    // Nouveaux attributs pour afficher les noms
    private String nomCategorie;
    private String nomUnite;

    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;

    private Long boutiqueId;

    public Long getBoutiqueId() {
        return boutiqueId;
    }

    public void setBoutiqueId(Long boutiqueId) {
        this.boutiqueId = boutiqueId;
    }
    
}
