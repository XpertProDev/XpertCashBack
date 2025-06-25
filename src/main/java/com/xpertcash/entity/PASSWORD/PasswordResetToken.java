package com.xpertcash.entity.PASSWORD;

import java.time.LocalDateTime;

import com.xpertcash.entity.User;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String token; 

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

}
