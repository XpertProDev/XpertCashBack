package com.xpertcash.DTOs;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatistiquesFactureReelleDTO {

    private Long totalFactures;
    private Double montantTotal;
    private Double montantTotalPaye;
    private Double montantTotalRestant;

    private Long nombreFacturesPayees;
    private Long nombreFacturesPartiellementPayees;
    private Long nombreFacturesEnAttente;
    private Double montantFacturesPayees;
    private Double montantFacturesPartiellementPayees;
    private Double montantFacturesEnAttente;

    private List<TopClientFactureDTO> topClients;
    private List<TopCreateurFactureDTO> topCreateurs;
    private String periode;
}
