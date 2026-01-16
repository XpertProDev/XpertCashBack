package com.xpertcash.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Unite;

@Repository
public interface UniteRepository extends JpaRepository<Unite, Long>{
    
    //  ATTENTION: Ces méthodes ne filtrent PAS par entreprise - dépréciées
    // Utiliser findByNomAndEntrepriseId ou existsByNomAndEntrepriseId à la place
    @Deprecated
    Optional<Unite> findByNom(String nom);
    
    @Deprecated
    boolean existsByNom(String nom);
    
    // Méthodes de filtrage par entreprise (isolantes)
    // Récupérer toutes les unités d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT u FROM Unite u " +
           "LEFT JOIN FETCH u.entreprise e " +
           "WHERE e.id = :entrepriseId")
    List<Unite> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
    
    // Recherche par nom et entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT u FROM Unite u " +
           "LEFT JOIN FETCH u.entreprise e " +
           "WHERE u.nom = :nom AND e.id = :entrepriseId")
    Optional<Unite> findByNomAndEntrepriseId(@Param("nom") String nom, @Param("entrepriseId") Long entrepriseId);
    
    // Vérifier l'existence par nom et entreprise (optimisé avec JOIN)
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Unite u " +
           "INNER JOIN u.entreprise e " +
           "WHERE u.nom = :nom AND e.id = :entrepriseId")
    boolean existsByNomAndEntrepriseId(@Param("nom") String nom, @Param("entrepriseId") Long entrepriseId);
    
    // Recherche par ID et entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT u FROM Unite u " +
           "LEFT JOIN FETCH u.entreprise e " +
           "WHERE u.id = :id AND e.id = :entrepriseId")
    Optional<Unite> findByIdAndEntrepriseId(@Param("id") Long id, @Param("entrepriseId") Long entrepriseId);

}
