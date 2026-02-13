package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopClientFactureDTO {
    private Long clientId;
    private String nomClient;
    private String type;
    private Long nombreFactures;
    private Double montantTotal;
}
