package com.xpertcash.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

public class EntrepriseClient {

     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String nom;
    private String pays;
    private String siege;
    private String adresse;
    private String email;
    private String telephone;
    private String secteur;
    private LocalDateTime createdAt;

    
    @OneToMany(mappedBy = "entrepriseClient", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("entrepriseClient")
    private List<Client> clients;

}
