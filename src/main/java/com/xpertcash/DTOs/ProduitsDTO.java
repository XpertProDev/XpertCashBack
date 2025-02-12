package com.xpertcash.DTOs;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProduitsDTO {
    private Long id;
    private String nomProduit;
    private String description;
    private Double prix;
    private String photo;
    private String quantite;
    private Integer seuil;
    private String alertSeuil;
    private LocalDateTime createdAt;
    private Long categoryId;
    private String nomCategory;
}

