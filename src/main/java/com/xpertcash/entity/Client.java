package com.xpertcash.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private LocalDateTime createdAt;
    
   @ManyToOne
//    @JsonBackReference
    @JoinColumn(name = "entreprise_client_id")
   @JsonIgnoreProperties("clientts")
    private EntrepriseClient entrepriseClient;
    

}
