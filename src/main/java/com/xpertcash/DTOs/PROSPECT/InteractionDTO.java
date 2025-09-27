package com.xpertcash.DTOs.PROSPECT;

import java.time.LocalDateTime;

import com.xpertcash.entity.Enum.PROSPECT.InteractionType;

public class InteractionDTO {
    public Long id;
    public InteractionType type;
    public LocalDateTime occurredAt;
    public String notes;
    public String assignedTo;
    public LocalDateTime nextFollowUp;

}
