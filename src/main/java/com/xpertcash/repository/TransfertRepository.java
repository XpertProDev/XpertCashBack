package com.xpertcash.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Transfert;

@Repository
public interface TransfertRepository extends JpaRepository<Transfert, Long> {
    List<Transfert> findByBoutiqueSourceIdOrBoutiqueDestinationId(Long boutiqueSourceId, Long boutiqueDestinationId);
    
    // Méthodes pour le résumé des transactions
    List<Transfert> findByBoutiqueSource_Entreprise_IdOrBoutiqueDestination_Entreprise_IdAndDateTransfertBetween(
        Long entrepriseId1, Long entrepriseId2, LocalDateTime dateDebut, LocalDateTime dateFin);
}
