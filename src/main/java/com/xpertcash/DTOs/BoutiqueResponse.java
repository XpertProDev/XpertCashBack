package com.xpertcash.DTOs;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BoutiqueResponse {
    private Long id;
    private String nomBoutique;
    private String adresse;
    private LocalDateTime createdAt;
}