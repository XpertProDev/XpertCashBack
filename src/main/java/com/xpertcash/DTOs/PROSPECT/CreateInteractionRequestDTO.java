package com.xpertcash.DTOs.PROSPECT;

import java.time.LocalDateTime;

import com.xpertcash.entity.Enum.PROSPECT.InteractionType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInteractionRequestDTO {
    private InteractionType type;
    private String notes;
    private String assignedTo;
    private LocalDateTime nextFollowUp;
}
