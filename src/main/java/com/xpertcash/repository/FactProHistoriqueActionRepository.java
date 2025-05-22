package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.FactProHistoriqueAction;

@Repository
public interface FactProHistoriqueActionRepository extends JpaRepository<FactProHistoriqueAction, Long> {

    List<FactProHistoriqueAction> findByFactureIdOrderByDateActionAsc(Long factureId);

    // Tri descendant (nouvelle méthode)
    List<FactProHistoriqueAction> findByFactureIdOrderByDateActionDesc(Long factureId);

}
