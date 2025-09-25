package com.xpertcash.DTOs.PROSPECT;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProspectAchatDTO {
    public Long id;
    public Long produitId;
    public String produitNom;
    public String typeProduit;
    public String descriptionProduit;
    public Double prixProduit;
    public Integer quantite;
    public Double montantAchat;
    public String notesAchat;
    public LocalDateTime dateAchat;
    public Long clientId;
    public String clientType;
}
