package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.CategorieDepense;

@Repository
public interface CategorieDepenseRepository extends JpaRepository<CategorieDepense, Long> {
    
    // Récupérer toutes les catégories de dépense d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT cd FROM CategorieDepense cd " +
           "LEFT JOIN FETCH cd.entreprise e " +
           "WHERE e.id = :entrepriseId")
    List<CategorieDepense> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
    
    // Recherche par ID et entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT cd FROM CategorieDepense cd " +
           "LEFT JOIN FETCH cd.entreprise e " +
           "WHERE cd.id = :id AND e.id = :entrepriseId")
    Optional<CategorieDepense> findByIdAndEntrepriseId(@Param("id") Long id, @Param("entrepriseId") Long entrepriseId);
    
    // Recherche par nom et entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT cd FROM CategorieDepense cd " +
           "LEFT JOIN FETCH cd.entreprise e " +
           "WHERE cd.nom = :nom AND e.id = :entrepriseId")
    Optional<CategorieDepense> findByNomAndEntrepriseId(@Param("nom") String nom, @Param("entrepriseId") Long entrepriseId);
    
    // Vérifier l'existence par nom et entreprise (optimisé avec JOIN)
    @Query("SELECT CASE WHEN COUNT(cd) > 0 THEN true ELSE false END FROM CategorieDepense cd " +
           "INNER JOIN cd.entreprise e " +
           "WHERE cd.nom = :nom AND e.id = :entrepriseId")
    boolean existsByNomAndEntrepriseId(@Param("nom") String nom, @Param("entrepriseId") Long entrepriseId);
}

