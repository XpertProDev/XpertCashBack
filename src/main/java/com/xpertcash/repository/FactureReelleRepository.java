package com.xpertcash.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.FactureReelle;

@Repository
public interface FactureReelleRepository extends JpaRepository<FactureReelle, Long> {

    Optional<FactureReelle> findTopByDateCreationOrderByNumeroFactureDesc(LocalDate dateCreation);
}

