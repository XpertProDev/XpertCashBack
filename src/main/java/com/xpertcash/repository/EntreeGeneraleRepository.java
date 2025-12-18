package com.xpertcash.repository;

import com.xpertcash.entity.EntreeGenerale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntreeGeneraleRepository extends JpaRepository<EntreeGenerale, Long> {
    List<EntreeGenerale> findByEntrepriseIdOrderByDateCreationDesc(Long entrepriseId);
    
    List<EntreeGenerale> findByEntrepriseId(Long entrepriseId);
    
    @Query("SELECT e FROM EntreeGenerale e WHERE e.entreprise.id = :entrepriseId " +
           "AND MONTH(e.dateCreation) = :month AND YEAR(e.dateCreation) = :year " +
           "ORDER BY e.numero DESC")
    List<EntreeGenerale> findByEntrepriseIdAndMonthAndYear(
            @Param("entrepriseId") Long entrepriseId,
            @Param("month") int month,
            @Param("year") int year);
    
    /**
     * Trouve une EntreeGenerale par son ID et l'ID de l'entreprise (sécurité)
     * Retourne Optional.empty() si l'entrée n'existe pas ou n'appartient pas à l'entreprise
     */
    @Query("SELECT e FROM EntreeGenerale e WHERE e.id = :id AND e.entreprise.id = :entrepriseId")
    Optional<EntreeGenerale> findByIdAndEntrepriseId(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);
}

