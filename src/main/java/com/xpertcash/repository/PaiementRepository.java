package com.xpertcash.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.Paiement;

public interface PaiementRepository extends JpaRepository<Paiement, Long>{

    // Calcule le total payé pour une facture
    @Query("SELECT COALESCE(SUM(p.montant), 0) FROM Paiement p WHERE p.factureReelle.id = :factureId")
    BigDecimal sumMontantsByFactureReelle(@Param("factureId") Long factureId);

    List<Paiement> findByFactureReelle(FactureReelle factureReelle);

    // Optimisation N+1 : Récupérer tous les paiements pour plusieurs factures
    @Query("SELECT p.factureReelle.id, COALESCE(SUM(p.montant), 0) FROM Paiement p " +
           "WHERE p.factureReelle.id IN :factureIds " +
           "GROUP BY p.factureReelle.id")
    List<Object[]> sumMontantsByFactureReelleIds(@Param("factureIds") List<Long> factureIds);

}
