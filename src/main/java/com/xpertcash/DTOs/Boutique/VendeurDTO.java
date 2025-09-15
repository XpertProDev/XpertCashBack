package com.xpertcash.DTOs.Boutique;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VendeurDTO {
    private long Id;
    private String nomComplet;
    private String email;
    private String phone;
    private String pays;
    private String photo;
    private LocalDateTime assignedAt;
    private String statut;

}
