package com.xpertcash.repository;

import com.xpertcash.entity.TransfertFonds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransfertFondsRepository extends JpaRepository<TransfertFonds, Long> {
    List<TransfertFonds> findByEntrepriseIdOrderByDateTransfertDesc(Long entrepriseId);
}

