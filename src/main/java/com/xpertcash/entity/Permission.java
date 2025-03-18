package com.xpertcash.entity;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 50) // Assurez-vous que la longueur est suffisante
    private PermissionType type;

}
