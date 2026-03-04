package com.xpertcash.DTOs;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.xpertcash.entity.ASSISTANCE.AssistanceStatus;

import lombok.Data;

@Data
public class AssistanceTicketDTO {

    private Long id;
    private String numeroTicket;
    private String sujet;
    private AssistanceStatus statut;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    private boolean deleted;

    private String createdByNom;
    private String createdByEmail;
    private String photo;

    private List<AssistanceMessageDTO> messages = new ArrayList<>();
}

