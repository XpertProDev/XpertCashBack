package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Transfert;

@Repository
public interface TransfertRepository extends JpaRepository<Transfert, Long> {
    List<Transfert> findByBoutiqueSourceIdOrBoutiqueDestinationId(Long boutiqueSourceId, Long boutiqueDestinationId);
}
