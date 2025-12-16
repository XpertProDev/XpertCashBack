package com.xpertcash.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetteItemDTO {

    private Long id;
    private String type;
    // private String source;
    private Double montantRestant;
    private LocalDateTime date;
    private String description;
    private String numero;
    private String client;
    private String contact;
}


