package com.xpertcash.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Unite;

@Repository
public interface UniteRepository extends JpaRepository<Unite, Long>{
    Optional<Unite> findByNom(String nom);
    
    boolean existsByNom(String nom);
    
    // Méthodes de filtrage par entreprise
    List<Unite> findByEntrepriseId(Long entrepriseId);
    
    Optional<Unite> findByNomAndEntrepriseId(String nom, Long entrepriseId);
    
    boolean existsByNomAndEntrepriseId(String nom, Long entrepriseId);
    
    Optional<Unite> findByIdAndEntrepriseId(Long id, Long entrepriseId);

}
