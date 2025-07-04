package com.xpertcash.entity.Module;

import java.time.LocalDateTime;

import com.xpertcash.entity.Entreprise;

import jakarta.persistence.*;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EntrepriseModuleAbonnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Entreprise entreprise;

    @ManyToOne
    private AppModule module;

    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;

    private boolean actif;

    public boolean estActif() {
        return actif && (dateFin == null || dateFin.isAfter(LocalDateTime.now()));
    }
}
