package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategorieDepenseDTO {
    private Long id;
    private String nom;
    private String description;
    private Long entrepriseId;
    private LocalDateTime createdAt;
}

