package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Categorie;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long>{
    
    // Méthodes isolantes (filtrées par entreprise)
    // Vérifier l'existence par nom et entreprise (optimisé avec JOIN)
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Categorie c " +
           "INNER JOIN c.entreprise e " +
           "WHERE c.nom = :nom AND e.id = :entrepriseId")
    boolean existsByNomAndEntrepriseId(@Param("nom") String nom, @Param("entrepriseId") Long entrepriseId);
    
    // Recherche par nom et entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT c FROM Categorie c " +
           "LEFT JOIN FETCH c.entreprise e " +
           "WHERE c.nom = :nom AND e.id = :entrepriseId")
    Categorie findByNomAndEntrepriseId(@Param("nom") String nom, @Param("entrepriseId") Long entrepriseId);

    // Méthode de pagination par entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT c FROM Categorie c " +
           "LEFT JOIN FETCH c.entreprise e " +
           "WHERE e.id = :entrepriseId")
    Page<Categorie> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId, Pageable pageable);
    
    // Méthode pour récupérer une catégorie par ID et entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT c FROM Categorie c " +
           "LEFT JOIN FETCH c.entreprise e " +
           "WHERE c.id = :id AND e.id = :entrepriseId")
    Optional<Categorie> findByIdAndEntrepriseId(@Param("id") Long id, @Param("entrepriseId") Long entrepriseId);
    
    // Méthode pour récupérer toutes les catégories d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT c FROM Categorie c " +
           "LEFT JOIN FETCH c.entreprise e " +
           "WHERE e.id = :entrepriseId")
    List<Categorie> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
}
