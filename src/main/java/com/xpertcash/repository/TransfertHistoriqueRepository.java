package com.xpertcash.repository;

import com.xpertcash.entity.TransfertHistorique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository // Annotation pour indiquer que c'est un repository géré par Spring
public interface TransfertHistoriqueRepository extends JpaRepository<TransfertHistorique, Long> {
    List<TransfertHistorique> findByMagasinId(Long magasinId);
}
