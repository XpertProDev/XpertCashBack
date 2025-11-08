package com.xpertcash.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xpertcash.entity.DepenseEntreprise;

public interface DepenseEntrepriseRepository extends JpaRepository<DepenseEntreprise, Long> {

    List<DepenseEntreprise> findByEntreprise_Id(Long entrepriseId);

    List<DepenseEntreprise> findByEntreprise_IdAndDateDepenseBetween(Long entrepriseId, LocalDateTime debut, LocalDateTime fin);
}

