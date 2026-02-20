package com.xpertcash.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Transfert;

@Repository
public interface TransfertRepository extends JpaRepository<Transfert, Long> {
    List<Transfert> findByBoutiqueSourceIdOrBoutiqueDestinationId(Long boutiqueSourceId, Long boutiqueDestinationId);

    /**
     * Tous les transferts dont la boutique source ou destination appartient à l'entreprise.
     * Appeler avec (entrepriseId, entrepriseId) pour l'isolation multi-tenant.
     */
    List<Transfert> findByBoutiqueSource_Entreprise_IdOrBoutiqueDestination_Entreprise_Id(Long entrepriseIdSource, Long entrepriseIdDestination);

    // Méthodes pour le résumé des transactions
    List<Transfert> findByBoutiqueSource_Entreprise_IdOrBoutiqueDestination_Entreprise_IdAndDateTransfertBetween(
        Long entrepriseId1, Long entrepriseId2, LocalDateTime dateDebut, LocalDateTime dateFin);
}
