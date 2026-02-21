package com.xpertcash.repository;

import com.xpertcash.entity.DepenseGenerale;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour la pagination comptabilité : requêtes UNION + LIMIT/OFFSET côté base.
 * (On étend Repository&lt;DepenseGenerale, Long&gt; pour que Spring crée le proxy ; les requêtes retournent Object[].)
 */
public interface ComptabilitePaginationRepository extends Repository<DepenseGenerale, Long> {

    /**
     * Page de transactions : chaque ligne = (tx_date, tx_type, entity_id).
     * Types : DEPENSE, ENTREE, FERMETURE_CAISSE, PAIEMENT, TRANSFERT_SORTIE, TRANSFERT_ENTREE.
     */
    @Query(value = """
        SELECT * FROM (
            SELECT d.date_creation AS tx_date, 'DEPENSE' AS tx_type, d.id AS entity_id FROM depense_generale d
            WHERE d.entreprise_id = :entrepriseId AND (d.designation NOT LIKE 'Transfert vers%%' AND d.designation NOT LIKE 'Transfert depuis%%')
            UNION ALL
            SELECT e.date_creation, 'ENTREE', e.id FROM entree_generale e
            WHERE e.entreprise_id = :entrepriseId AND (e.dette_type IS NULL OR e.dette_type != 'PAIEMENT_FACTURE') AND (e.designation NOT LIKE 'Transfert vers%%' AND e.designation NOT LIKE 'Transfert depuis%%')
            UNION ALL
            SELECT c.date_fermeture, 'FERMETURE_CAISSE', c.id FROM caisse c INNER JOIN boutique b ON c.boutique_id = b.id
            WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND EXISTS (SELECT 1 FROM vente v WHERE v.caisse_id = c.id)
            UNION ALL
            SELECT p.date_paiement, 'PAIEMENT', p.id FROM paiement p INNER JOIN facture_reelle f ON p.facture_reelle_id = f.id WHERE f.entreprise_id = :entrepriseId
            UNION ALL
            SELECT t.date_transfert, 'TRANSFERT_SORTIE', t.id FROM transfert_fonds t WHERE t.entreprise_id = :entrepriseId
            UNION ALL
            SELECT t.date_transfert, 'TRANSFERT_ENTREE', t.id FROM transfert_fonds t WHERE t.entreprise_id = :entrepriseId
        ) AS u ORDER BY u.tx_date DESC LIMIT :lim OFFSET :off
        """, nativeQuery = true)
    List<Object[]> findTransactionPage(
            @Param("entrepriseId") Long entrepriseId,
            @Param("lim") int limit,
            @Param("off") int offset);

    /**
     * Nombre total de transactions (même périmètre que findTransactionPage).
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT 1 FROM depense_generale d WHERE d.entreprise_id = :entrepriseId AND (d.designation NOT LIKE 'Transfert vers%%' AND d.designation NOT LIKE 'Transfert depuis%%')
            UNION ALL SELECT 1 FROM entree_generale e WHERE e.entreprise_id = :entrepriseId AND (e.dette_type IS NULL OR e.dette_type != 'PAIEMENT_FACTURE') AND (e.designation NOT LIKE 'Transfert vers%%' AND e.designation NOT LIKE 'Transfert depuis%%')
            UNION ALL SELECT 1 FROM caisse c INNER JOIN boutique b ON c.boutique_id = b.id WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND EXISTS (SELECT 1 FROM vente v WHERE v.caisse_id = c.id)
            UNION ALL SELECT 1 FROM paiement p INNER JOIN facture_reelle f ON p.facture_reelle_id = f.id WHERE f.entreprise_id = :entrepriseId
            UNION ALL SELECT 1 FROM transfert_fonds t WHERE t.entreprise_id = :entrepriseId
            UNION ALL SELECT 1 FROM transfert_fonds t WHERE t.entreprise_id = :entrepriseId
        ) AS c
        """, nativeQuery = true)
    long countTransactions(@Param("entrepriseId") Long entrepriseId);

    /**
     * Page de transactions filtrée par période (même filtre que trésorerie).
     * dateDebut inclus, dateFin exclu (ex: [dateDebut, dateFin[).
     */
    @Query(value = """
        SELECT * FROM (
            SELECT d.date_creation AS tx_date, 'DEPENSE' AS tx_type, d.id AS entity_id FROM depense_generale d
            WHERE d.entreprise_id = :entrepriseId AND (d.designation NOT LIKE 'Transfert vers%%' AND d.designation NOT LIKE 'Transfert depuis%%')
            AND d.date_creation >= :dateDebut AND d.date_creation < :dateFin
            UNION ALL
            SELECT e.date_creation, 'ENTREE', e.id FROM entree_generale e
            WHERE e.entreprise_id = :entrepriseId AND (e.dette_type IS NULL OR e.dette_type != 'PAIEMENT_FACTURE') AND (e.designation NOT LIKE 'Transfert vers%%' AND e.designation NOT LIKE 'Transfert depuis%%')
            AND e.date_creation >= :dateDebut AND e.date_creation < :dateFin
            UNION ALL
            SELECT c.date_fermeture, 'FERMETURE_CAISSE', c.id FROM caisse c INNER JOIN boutique b ON c.boutique_id = b.id
            WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND EXISTS (SELECT 1 FROM vente v WHERE v.caisse_id = c.id)
            AND c.date_fermeture >= :dateDebut AND c.date_fermeture < :dateFin
            UNION ALL
            SELECT p.date_paiement, 'PAIEMENT', p.id FROM paiement p INNER JOIN facture_reelle f ON p.facture_reelle_id = f.id WHERE f.entreprise_id = :entrepriseId
            AND p.date_paiement >= :dateDebut AND p.date_paiement < :dateFin
            UNION ALL
            SELECT t.date_transfert, 'TRANSFERT_SORTIE', t.id FROM transfert_fonds t WHERE t.entreprise_id = :entrepriseId
            AND t.date_transfert >= :dateDebut AND t.date_transfert < :dateFin
            UNION ALL
            SELECT t.date_transfert, 'TRANSFERT_ENTREE', t.id FROM transfert_fonds t WHERE t.entreprise_id = :entrepriseId
            AND t.date_transfert >= :dateDebut AND t.date_transfert < :dateFin
        ) AS u ORDER BY u.tx_date DESC LIMIT :lim OFFSET :off
        """, nativeQuery = true)
    List<Object[]> findTransactionPageWithPeriod(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("lim") int limit,
            @Param("off") int offset);

    /**
     * Nombre total de transactions sur la période (même périmètre que findTransactionPageWithPeriod).
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT 1 FROM depense_generale d WHERE d.entreprise_id = :entrepriseId AND (d.designation NOT LIKE 'Transfert vers%%' AND d.designation NOT LIKE 'Transfert depuis%%') AND d.date_creation >= :dateDebut AND d.date_creation < :dateFin
            UNION ALL SELECT 1 FROM entree_generale e WHERE e.entreprise_id = :entrepriseId AND (e.dette_type IS NULL OR e.dette_type != 'PAIEMENT_FACTURE') AND (e.designation NOT LIKE 'Transfert vers%%' AND e.designation NOT LIKE 'Transfert depuis%%') AND e.date_creation >= :dateDebut AND e.date_creation < :dateFin
            UNION ALL SELECT 1 FROM caisse c INNER JOIN boutique b ON c.boutique_id = b.id WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND EXISTS (SELECT 1 FROM vente v WHERE v.caisse_id = c.id) AND c.date_fermeture >= :dateDebut AND c.date_fermeture < :dateFin
            UNION ALL SELECT 1 FROM paiement p INNER JOIN facture_reelle f ON p.facture_reelle_id = f.id WHERE f.entreprise_id = :entrepriseId AND p.date_paiement >= :dateDebut AND p.date_paiement < :dateFin
            UNION ALL SELECT 1 FROM transfert_fonds t WHERE t.entreprise_id = :entrepriseId AND t.date_transfert >= :dateDebut AND t.date_transfert < :dateFin
            UNION ALL SELECT 1 FROM transfert_fonds t WHERE t.entreprise_id = :entrepriseId AND t.date_transfert >= :dateDebut AND t.date_transfert < :dateFin
        ) AS c
        """, nativeQuery = true)
    long countTransactionsWithPeriod(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);
}
