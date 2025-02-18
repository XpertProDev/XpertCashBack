package com.xpertcash.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.UniteMesure;

@Repository
public interface UniteMesureRepository extends JpaRepository<UniteMesure, Long> {
    // Méthode pour récupérer une unité de mesure par son nom
    Optional<UniteMesure> findByNomUnite(String nomUnite);
}
