package com.xpertcash.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.FactureReelle;

@Repository
public interface FactureReelleRepository extends JpaRepository<FactureReelle, Long> {

    Optional<FactureReelle> findTopByDateCreationOrderByNumeroFactureDesc(LocalDate dateCreation);

    @Query("SELECT f FROM FactureReelle f WHERE FUNCTION('YEAR', f.dateCreation) = :year ORDER BY f.numeroFacture DESC")
    List<FactureReelle> findFacturesDeLAnnee(@Param("year") int year);
    

    List<FactureReelle> findByEntreprise(Entreprise entreprise);

    // Trie

    @Query("SELECT f FROM FactureReelle f WHERE MONTH(f.dateCreation) = :mois AND YEAR(f.dateCreation) = :annee")
    List<FactureReelle> findByMonthAndYear(@Param("mois") Integer mois, @Param("annee") Integer annee);

    @Query("SELECT f FROM FactureReelle f WHERE MONTH(f.dateCreation) = :mois")
    List<FactureReelle> findByMonth(@Param("mois") Integer mois);

    @Query("SELECT f FROM FactureReelle f WHERE YEAR(f.dateCreation) = :annee")
    List<FactureReelle> findByYear(@Param("annee") Integer annee);



}

