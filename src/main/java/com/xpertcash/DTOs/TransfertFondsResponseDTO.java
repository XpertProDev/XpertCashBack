package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransfertFondsResponseDTO {
    private Long id;
    private LocalDateTime dateTransfert;
    private String motif;
    private String responsable;
    private String de;
    private String vers;
    private Double montant;
}

