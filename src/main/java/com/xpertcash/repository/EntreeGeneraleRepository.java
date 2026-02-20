package com.xpertcash.repository;

import com.xpertcash.entity.EntreeGenerale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntreeGeneraleRepository extends JpaRepository<EntreeGenerale, Long> {
    
    // Récupérer toutes les entrées générales d'une entreprise triées par date (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT e FROM EntreeGenerale e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "LEFT JOIN FETCH e.categorie " +
           "WHERE ent.id = :entrepriseId " +
           "ORDER BY e.dateCreation DESC")
    List<EntreeGenerale> findByEntrepriseIdOrderByDateCreationDesc(@Param("entrepriseId") Long entrepriseId);
    
    // Récupérer toutes les entrées générales d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT e FROM EntreeGenerale e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "LEFT JOIN FETCH e.categorie " +
           "WHERE ent.id = :entrepriseId")
    List<EntreeGenerale> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
    
    // Récupérer les entrées générales par entreprise, mois et année (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT e FROM EntreeGenerale e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "LEFT JOIN FETCH e.categorie " +
           "WHERE ent.id = :entrepriseId " +
           "AND MONTH(e.dateCreation) = :month AND YEAR(e.dateCreation) = :year " +
           "ORDER BY e.numero DESC")
    List<EntreeGenerale> findByEntrepriseIdAndMonthAndYear(
            @Param("entrepriseId") Long entrepriseId,
            @Param("month") int month,
            @Param("year") int year);
    
    /**
     * Trouve une EntreeGenerale par son ID et l'ID de l'entreprise (sécurité)
     * Retourne Optional.empty() si l'entrée n'existe pas ou n'appartient pas à l'entreprise
     * Optimisé avec JOIN FETCH pour charger les relations
     */
    @Query("SELECT e FROM EntreeGenerale e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "LEFT JOIN FETCH e.categorie " +
           "WHERE e.id = :id AND ent.id = :entrepriseId")
    Optional<EntreeGenerale> findByIdAndEntrepriseId(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);
    
    // Récupérer les entrées générales d'une entreprise dans une période (optimisé)
    @Query("SELECT DISTINCT e FROM EntreeGenerale e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "LEFT JOIN FETCH e.categorie " +
           "WHERE ent.id = :entrepriseId " +
           "AND e.dateCreation >= :dateDebut AND e.dateCreation < :dateFin " +
           "ORDER BY e.dateCreation DESC")
    List<EntreeGenerale> findByEntrepriseIdAndDateCreationBetween(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    /** Pour pagination scalable : charger par IDs avec relations (évite N+1). */
    @Query("SELECT DISTINCT e FROM EntreeGenerale e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "LEFT JOIN FETCH e.categorie " +
           "LEFT JOIN FETCH e.creePar " +
           "LEFT JOIN FETCH e.responsable " +
           "WHERE e.id IN :ids")
    List<EntreeGenerale> findByIdInWithDetails(@Param("ids") List<Long> ids);
}

