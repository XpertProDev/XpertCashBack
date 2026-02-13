package com.xpertcash.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    // Récupérer tous les paiements pour un ensemble de factures
    List<Paiement> findByFactureReelle_IdIn(List<Long> factureIds);

    // Optimisation N+1 : Récupérer tous les paiements pour plusieurs factures
    @Query("SELECT p.factureReelle.id, COALESCE(SUM(p.montant), 0) FROM Paiement p " +
           "WHERE p.factureReelle.id IN :factureIds " +
           "GROUP BY p.factureReelle.id")
    List<Object[]> sumMontantsByFactureReelleIds(@Param("factureIds") List<Long> factureIds);

    // Récupère tous les paiements d'une entreprise
    @Query("SELECT p FROM Paiement p WHERE p.factureReelle.entreprise.id = :entrepriseId ORDER BY p.datePaiement DESC")
    List<Paiement> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Récupère tous les paiements d'une entreprise dans une période (optimisé)
    @Query("SELECT p FROM Paiement p WHERE p.factureReelle.entreprise.id = :entrepriseId " +
           "AND p.datePaiement >= :dateDebut AND p.datePaiement < :dateFin " +
           "ORDER BY p.datePaiement DESC")
    List<Paiement> findByEntrepriseIdAndDatePaiementBetween(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    // Total payé pour les factures créées dans une période
    @Query(value = "SELECT COALESCE(SUM(p.montant), 0) FROM paiement p " +
           "INNER JOIN facture_reelle f ON p.facture_reelle_id = f.id " +
           "WHERE f.entreprise_id = :entrepriseId " +
           "AND f.date_creation >= :dateDebut AND f.date_creation < :dateFin", nativeQuery = true)
    java.math.BigDecimal sumMontantsByEntrepriseIdAndPeriodeFacture(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") java.time.LocalDate dateDebut,
            @Param("dateFin") java.time.LocalDate dateFin);

}
