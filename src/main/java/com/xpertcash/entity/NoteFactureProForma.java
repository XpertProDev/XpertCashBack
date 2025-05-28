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
public class NoteFactureProForma {

     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private FactureProForma facture;

    @ManyToOne
    private User auteur;

    private String contenu;

    private LocalDateTime dateCreation;

    private boolean modifiee = false;

    private String numeroIdentifiant;

    private LocalDateTime dateDerniereModification;

}
