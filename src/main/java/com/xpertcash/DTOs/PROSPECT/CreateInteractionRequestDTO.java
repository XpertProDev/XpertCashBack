package com.xpertcash.DTOs.PROSPECT;

import java.time.LocalDateTime;

import com.xpertcash.entity.Enum.PROSPECT.InteractionType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateInteractionRequestDTO {
    private InteractionType type;
    private String notes;
    private String assignedTo;
    private LocalDateTime nextFollowUp;
    private Long produitId;
}
