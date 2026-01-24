package com.xpertcash.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.DepenseGenerale;

@Repository
public interface DepenseGeneraleRepository extends JpaRepository<DepenseGenerale, Long> {
    
    // Récupérer toutes les dépenses générales d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT d FROM DepenseGenerale d " +
           "LEFT JOIN FETCH d.entreprise e " +
           "LEFT JOIN FETCH d.categorie " +
           "LEFT JOIN FETCH d.categorieLiee " +
           "WHERE e.id = :entrepriseId")
    List<DepenseGenerale> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
    
    // Récupérer toutes les dépenses générales d'une entreprise triées par date (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT d FROM DepenseGenerale d " +
           "LEFT JOIN FETCH d.entreprise e " +
           "LEFT JOIN FETCH d.categorie " +
           "LEFT JOIN FETCH d.categorieLiee " +
           "WHERE e.id = :entrepriseId " +
           "ORDER BY d.dateCreation DESC")
    List<DepenseGenerale> findByEntrepriseIdOrderByDateCreationDesc(@Param("entrepriseId") Long entrepriseId);
    
    // Récupérer les dépenses générales par entreprise, mois et année (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT d FROM DepenseGenerale d " +
           "LEFT JOIN FETCH d.entreprise e " +
           "LEFT JOIN FETCH d.categorie " +
           "LEFT JOIN FETCH d.categorieLiee " +
           "WHERE e.id = :entrepriseId " +
           "AND MONTH(d.dateCreation) = :month " +
           "AND YEAR(d.dateCreation) = :year " +
           "ORDER BY d.numero DESC")
    List<DepenseGenerale> findByEntrepriseIdAndMonthAndYear(
        @Param("entrepriseId") Long entrepriseId,
        @Param("month") int month,
        @Param("year") int year
    );
    
    // Récupérer les dépenses générales d'une entreprise dans une période (optimisé)
    @Query("SELECT DISTINCT d FROM DepenseGenerale d " +
           "LEFT JOIN FETCH d.entreprise e " +
           "LEFT JOIN FETCH d.categorie " +
           "LEFT JOIN FETCH d.categorieLiee " +
           "WHERE e.id = :entrepriseId " +
           "AND d.dateCreation >= :dateDebut AND d.dateCreation < :dateFin " +
           "ORDER BY d.dateCreation DESC")
    List<DepenseGenerale> findByEntrepriseIdAndDateCreationBetween(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);
}

