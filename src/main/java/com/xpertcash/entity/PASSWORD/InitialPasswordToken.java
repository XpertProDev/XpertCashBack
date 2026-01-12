package com.xpertcash.entity.PASSWORD;

import java.time.LocalDateTime;

import com.xpertcash.entity.User;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class InitialPasswordToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token; 

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @Column(nullable = false)
    private String generatedPassword; // Mot de passe généré (sera supprimé après utilisation)

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

}

