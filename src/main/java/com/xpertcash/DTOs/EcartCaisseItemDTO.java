package com.xpertcash.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Détail d'une dette écart caisse : uniquement les champs nécessaires (pas de dateCreationFormatee ni designationAvecDateFr). */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EcartCaisseItemDTO {
    private Long id;
    private String numero;
    private String designation;
    private LocalDateTime dateCreation;
    private Double montant;
    private Double montantReste;
    private Long caisseId;
    private String detteNumero;
}
