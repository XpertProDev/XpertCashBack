package com.xpertcash.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor

public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nomComplet;
    private String adresse;
    private String telephone;
    private String email;
    
   @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "entreprise_client_id")
    private EntrepriseClient entrepriseClient;

}
