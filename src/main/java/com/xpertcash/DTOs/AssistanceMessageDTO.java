package com.xpertcash.DTOs;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AssistanceMessageDTO {

    private Long id;
    private Long ticketId;
    private Long auteurId;
    private String auteurNom;
    private String auteurPhoto;
    private String contenu;
    private String pieceJointePath;
    private boolean support;
    private LocalDateTime createdAt;
}

