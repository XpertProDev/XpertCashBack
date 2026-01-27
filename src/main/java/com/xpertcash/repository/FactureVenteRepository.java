package com.xpertcash.repository;

import com.xpertcash.entity.FactureVente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactureVenteRepository extends JpaRepository<FactureVente, Long> {
    
    // Récupérer toutes les factures de vente d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureVente f " +
           "LEFT JOIN FETCH f.vente v " +
           "LEFT JOIN FETCH v.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE e.id = :entrepriseId")
    List<FactureVente> findAllByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Récupérer les factures de vente d'une entreprise filtrées par période (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureVente f " +
           "LEFT JOIN FETCH f.vente v " +
           "LEFT JOIN FETCH v.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND f.dateEmission >= :dateDebut " +
           "AND f.dateEmission < :dateFin")
    List<FactureVente> findAllByEntrepriseIdAndDateEmissionBetween(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    // Recherche par vente et entreprise (pour isolation)
    @Query("SELECT DISTINCT f FROM FactureVente f " +
           "LEFT JOIN FETCH f.vente v " +
           "LEFT JOIN FETCH v.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE v.id = :venteId " +
           "AND e.id = :entrepriseId")
    Optional<FactureVente> findByVenteIdAndEntrepriseId(
            @Param("venteId") Long venteId,
            @Param("entrepriseId") Long entrepriseId);

}