package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor

public class BoutiqueResponse {
    private Long id;
    private String nomBoutique;
    private String adresse;
    private String telephone;
    private String email;
    private LocalDateTime createdAt;
    private boolean actif = true;
}