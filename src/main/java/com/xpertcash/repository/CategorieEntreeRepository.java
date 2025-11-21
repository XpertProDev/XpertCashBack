package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.CategorieEntree;

@Repository
public interface CategorieEntreeRepository extends JpaRepository<CategorieEntree, Long> {
    List<CategorieEntree> findByEntrepriseId(Long entrepriseId);
    
    Optional<CategorieEntree> findByIdAndEntrepriseId(Long id, Long entrepriseId);
    
    Optional<CategorieEntree> findByNomAndEntrepriseId(String nom, Long entrepriseId);
    
    boolean existsByNomAndEntrepriseId(String nom, Long entrepriseId);
}

