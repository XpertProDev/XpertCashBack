package com.xpertcash.entity.PROSPECT;

import java.time.LocalDateTime;

import com.xpertcash.entity.Enum.PROSPECT.InteractionType;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor

@Table(name = "interactions")
public class Interaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private InteractionType type;

    private LocalDateTime occurredAt = LocalDateTime.now();

    @Column(length = 2000)
    private String notes;
    private String assignedTo; // commercial qui a fait l'action

    private LocalDateTime nextFollowUp; // nullable

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospect_id")
    private Prospect prospect;

}
