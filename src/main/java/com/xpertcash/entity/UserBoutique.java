package com.xpertcash.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBoutique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "boutique_id", nullable = false)
    private Boutique boutique;

    private LocalDateTime assignedAt;

    // @Column(name = "is_gestionnaire_stock", nullable = false)
    // private boolean isGestionnaireStock = false;

    @Column(nullable = false)
    private boolean canGestionStock = false;

    @Column(nullable = false)
    private boolean canGererProduits = false;
}

