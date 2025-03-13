package com.xpertcash.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Facture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String type;
    private Integer quantite;
    private String description;
    private LocalDateTime dateFacture;
    private String numeroFacture;

     @ManyToOne
     @JoinColumn(name = "user_id")
     private User user;

     @ManyToOne
     @JoinColumn(name = "produit_id")
     private Produit produit;

}
