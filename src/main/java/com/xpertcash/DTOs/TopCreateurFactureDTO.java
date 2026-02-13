package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopCreateurFactureDTO {
    private Long createurId;
    private String nomCreateur;
    private Long nombreFactures;
    private Double montantTotal;
}
