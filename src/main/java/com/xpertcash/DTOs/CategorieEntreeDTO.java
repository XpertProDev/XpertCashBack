package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategorieEntreeDTO {
    private Long id;
    private String nom;
    private String description;
    private String origineCreation;
    private Long entrepriseId;
    private LocalDateTime createdAt;
}

