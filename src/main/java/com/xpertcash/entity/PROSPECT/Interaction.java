package com.xpertcash.entity.PROSPECT;

import java.time.LocalDateTime;

import com.xpertcash.entity.Enum.PROSPECT.InteractionType;
import com.xpertcash.entity.Produit;

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
    private String assignedTo;
    private LocalDateTime nextFollowUp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospect_id")
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Prospect prospect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = true)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Produit produit;

}
