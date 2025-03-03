package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor


public class BoutiqueResponse {
    private Long id;
    private String nomBoutique;
    private String adresse;
    private LocalDateTime createdAt;
}