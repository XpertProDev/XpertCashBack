package com.xpertcash.repository;

import com.xpertcash.entity.TransfertFonds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransfertFondsRepository extends JpaRepository<TransfertFonds, Long> {
    List<TransfertFonds> findByEntrepriseIdOrderByDateTransfertDesc(Long entrepriseId);

    @Query("SELECT t FROM TransfertFonds t LEFT JOIN FETCH t.creePar LEFT JOIN FETCH t.entreprise WHERE t.id IN :ids")
    List<TransfertFonds> findAllByIdWithDetails(@Param("ids") List<Long> ids);
}

