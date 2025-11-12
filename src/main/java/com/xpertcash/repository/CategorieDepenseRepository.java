package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.CategorieDepense;

@Repository
public interface CategorieDepenseRepository extends JpaRepository<CategorieDepense, Long> {
    List<CategorieDepense> findByEntrepriseId(Long entrepriseId);
    
    Optional<CategorieDepense> findByIdAndEntrepriseId(Long id, Long entrepriseId);
    
    Optional<CategorieDepense> findByNomAndEntrepriseId(String nom, Long entrepriseId);
    
    boolean existsByNomAndEntrepriseId(String nom, Long entrepriseId);
}

