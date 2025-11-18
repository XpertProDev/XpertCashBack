package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.DepenseGenerale;

@Repository
public interface DepenseGeneraleRepository extends JpaRepository<DepenseGenerale, Long> {
    List<DepenseGenerale> findByEntrepriseId(Long entrepriseId);
    
    List<DepenseGenerale> findByEntrepriseIdOrderByDateCreationDesc(Long entrepriseId);
    
    @Query("SELECT d FROM DepenseGenerale d WHERE d.entreprise.id = :entrepriseId " +
           "AND MONTH(d.dateCreation) = :month " +
           "AND YEAR(d.dateCreation) = :year " +
           "ORDER BY d.numero DESC")
    List<DepenseGenerale> findByEntrepriseIdAndMonthAndYear(
        @Param("entrepriseId") Long entrepriseId,
        @Param("month") int month,
        @Param("year") int year
    );
}

