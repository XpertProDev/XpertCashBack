package com.xpertcash.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String adresse;
    private String email;
    private String telephone;

    
    @OneToMany(mappedBy = "entrepriseClient", cascade = CascadeType.ALL)
    private List<Client> clients;

}
