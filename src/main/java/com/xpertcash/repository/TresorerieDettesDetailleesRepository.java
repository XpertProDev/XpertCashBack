package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xpertcash.entity.VENTE.Vente;

/**
 * Pagination côté base pour les dettes détaillées (UNION factures impayées, dépenses DETTE, entrées DETTE, ventes à crédit).
 * Méthodes implémentées par Spring Data via @Query native.
 */
public interface TresorerieDettesDetailleesRepository extends JpaRepository<Vente, Long> {

    @Query(value = """
        SELECT * FROM (
            SELECT COALESCE(f.date_creation_pro, CAST(f.date_creation AS DATETIME)) AS tx_date, 'FACTURE_IMPAYEE' AS tx_type, f.id AS entity_id
            FROM facture_reelle f
            WHERE f.entreprise_id = :entrepriseId
              AND (f.total_facture - (SELECT COALESCE(SUM(p.montant), 0) FROM paiement p WHERE p.facture_reelle_id = f.id)) > 0
            UNION ALL
            SELECT d.date_creation, 'DEPENSE_DETTE', d.id FROM depense_generale d
            WHERE d.entreprise_id = :entrepriseId AND d.source = 'DETTE'
            UNION ALL
            SELECT e.date_creation, 'ENTREE_DETTE', e.id FROM entree_generale e
            WHERE e.entreprise_id = :entrepriseId AND e.source = 'DETTE' AND (e.dette_type IS NULL OR e.dette_type != 'PAIEMENT_FACTURE')
            UNION ALL
            SELECT v.date_vente, 'VENTE_CREDIT', v.id FROM vente v
            INNER JOIN caisse c ON v.caisse_id = c.id
            INNER JOIN boutique b ON v.boutique_id = b.id
            WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND v.mode_paiement = 'CREDIT'
              AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0
        ) AS u ORDER BY u.tx_date DESC LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findDettesDetailleesPage(
            @Param("entrepriseId") Long entrepriseId,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT 1 FROM facture_reelle f
            WHERE f.entreprise_id = :entrepriseId
              AND (f.total_facture - (SELECT COALESCE(SUM(p.montant), 0) FROM paiement p WHERE p.facture_reelle_id = f.id)) > 0
            UNION ALL
            SELECT 1 FROM depense_generale d WHERE d.entreprise_id = :entrepriseId AND d.source = 'DETTE'
            UNION ALL
            SELECT 1 FROM entree_generale e WHERE e.entreprise_id = :entrepriseId AND e.source = 'DETTE' AND (e.dette_type IS NULL OR e.dette_type != 'PAIEMENT_FACTURE')
            UNION ALL
            SELECT 1 FROM vente v INNER JOIN caisse c ON v.caisse_id = c.id INNER JOIN boutique b ON v.boutique_id = b.id
            WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND v.mode_paiement = 'CREDIT'
              AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0
        ) AS c
        """, nativeQuery = true)
    long countDettesDetaillees(@Param("entrepriseId") Long entrepriseId);

    /**
     * Page de dettes détaillées avec recherche plein texte (numero, client, vendeurNom, description).
     */
    @Query(value = """
        SELECT * FROM (
            SELECT COALESCE(f.date_creation_pro, CAST(f.date_creation AS DATETIME)) AS tx_date,
                   'FACTURE_IMPAYEE' AS tx_type,
                   f.id AS entity_id
            FROM facture_reelle f
            LEFT JOIN client cl ON f.client_id = cl.id
            LEFT JOIN entreprise_client ec ON f.entreprise_client_id = ec.id
            WHERE f.entreprise_id = :entrepriseId
              AND (f.total_facture - (SELECT COALESCE(SUM(p.montant), 0) FROM paiement p WHERE p.facture_reelle_id = f.id)) > 0
              AND (
                  LOWER(COALESCE(f.numero_facture, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(f.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(cl.nom_complet, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(ec.nom, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            UNION ALL
            SELECT d.date_creation, 'DEPENSE_DETTE', d.id
            FROM depense_generale d
            LEFT JOIN fournisseur fr ON d.fournisseur_id = fr.id
            WHERE d.entreprise_id = :entrepriseId
              AND d.source = 'DETTE'
              AND (
                  LOWER(COALESCE(d.numero, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(d.designation, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(fr.nom_complet, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            UNION ALL
            SELECT e.date_creation, 'ENTREE_DETTE', e.id
            FROM entree_generale e
            LEFT JOIN user u ON e.responsable_id = u.id
            WHERE e.entreprise_id = :entrepriseId
              AND e.source = 'DETTE'
              AND (e.dette_type IS NULL OR e.dette_type != 'PAIEMENT_FACTURE')
              AND (
                  LOWER(COALESCE(e.numero, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(e.designation, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(u.nom_complet, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            UNION ALL
            SELECT v.date_vente, 'VENTE_CREDIT', v.id
            FROM vente v
            INNER JOIN caisse c ON v.caisse_id = c.id
            INNER JOIN boutique b ON v.boutique_id = b.id
            LEFT JOIN client cl2 ON v.client_id = cl2.id
            LEFT JOIN entreprise_client ec2 ON v.entreprise_client_id = ec2.id
            LEFT JOIN user u2 ON v.vendeur_id = u2.id
            WHERE b.entreprise_id = :entrepriseId
              AND c.statut = 'FERMEE'
              AND v.mode_paiement = 'CREDIT'
              AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0
              AND (
                  LOWER(COALESCE(v.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(cl2.nom_complet, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(ec2.nom, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(v.client_nom, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(u2.nom_complet, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
        ) AS u ORDER BY u.tx_date DESC LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findDettesDetailleesPageWithSearch(
            @Param("entrepriseId") Long entrepriseId,
            @Param("search") String search,
            @Param("limit") int limit,
            @Param("offset") int offset);

    /**
     * Nombre total de dettes détaillées pour la recherche (même périmètre que findDettesDetailleesPageWithSearch).
     */
    @Query(value = """
        SELECT COUNT(*) FROM (
            SELECT 1
            FROM facture_reelle f
            LEFT JOIN client cl ON f.client_id = cl.id
            LEFT JOIN entreprise_client ec ON f.entreprise_client_id = ec.id
            WHERE f.entreprise_id = :entrepriseId
              AND (f.total_facture - (SELECT COALESCE(SUM(p.montant), 0) FROM paiement p WHERE p.facture_reelle_id = f.id)) > 0
              AND (
                  LOWER(COALESCE(f.numero_facture, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(f.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(cl.nom_complet, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(ec.nom, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            UNION ALL
            SELECT 1
            FROM depense_generale d
            LEFT JOIN fournisseur fr ON d.fournisseur_id = fr.id
            WHERE d.entreprise_id = :entrepriseId
              AND d.source = 'DETTE'
              AND (
                  LOWER(COALESCE(d.numero, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(d.designation, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(fr.nom_complet, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            UNION ALL
            SELECT 1
            FROM entree_generale e
            LEFT JOIN user u ON e.responsable_id = u.id
            WHERE e.entreprise_id = :entrepriseId
              AND e.source = 'DETTE'
              AND (e.dette_type IS NULL OR e.dette_type != 'PAIEMENT_FACTURE')
              AND (
                  LOWER(COALESCE(e.numero, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(e.designation, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(u.nom_complet, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            UNION ALL
            SELECT 1
            FROM vente v
            INNER JOIN caisse c ON v.caisse_id = c.id
            INNER JOIN boutique b ON v.boutique_id = b.id
            LEFT JOIN client cl2 ON v.client_id = cl2.id
            LEFT JOIN entreprise_client ec2 ON v.entreprise_client_id = ec2.id
            LEFT JOIN user u2 ON v.vendeur_id = u2.id
            WHERE b.entreprise_id = :entrepriseId
              AND c.statut = 'FERMEE'
              AND v.mode_paiement = 'CREDIT'
              AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0
              AND (
                  LOWER(COALESCE(v.description, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(cl2.nom_complet, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(ec2.nom, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(v.client_nom, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                  OR LOWER(COALESCE(u2.nom_complet, '')) LIKE LOWER(CONCAT('%', :search, '%'))
              )
        ) AS c
        """, nativeQuery = true)
    long countDettesDetailleesWithSearch(
            @Param("entrepriseId") Long entrepriseId,
            @Param("search") String search);
}
