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

    @Query("SELECT f FROM FactureReelle f WHERE FUNCTION('MONTH', f.dateCreation) = :month AND FUNCTION('YEAR', f.dateCreation) = :year ORDER BY f.numeroFacture DESC")
    List<FactureReelle> findFacturesDuMois(@Param("month") int month, @Param("year") int year);

    List<FactureReelle> findByEntreprise(Entreprise entreprise);


}

