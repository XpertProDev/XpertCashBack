package com.xpertcash.repository.VENTE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xpertcash.entity.VENTE.VenteProduit;

public interface VenteProduitRepository extends JpaRepository<VenteProduit, Long> {

    Optional<VenteProduit> findByVenteIdAndProduitId(Long venteId, Long produitId);

    // Récupérer les 3 meilleurs produits vendus par entreprise (optimisé avec requête native)
    // Exclut les produits remboursés et groupe par produit
    // Optimisé pour de grandes quantités de données avec index sur les clés étrangères
    @Query(value = "SELECT vp.produit_id, p.nom, " +
           "SUM(CASE WHEN vp.est_remboursee = true AND vp.quantite_remboursee IS NOT NULL " +
           "THEN vp.quantite - vp.quantite_remboursee ELSE vp.quantite END) as total_quantite, " +
           "SUM(CASE WHEN vp.est_remboursee = true AND vp.montant_rembourse IS NOT NULL " +
           "THEN vp.montant_ligne - vp.montant_rembourse ELSE vp.montant_ligne END) as total_montant " +
           "FROM vente_produit vp " +
           "INNER JOIN vente v ON vp.vente_id = v.id " +
           "INNER JOIN boutique b ON v.boutique_id = b.id " +
           "INNER JOIN entreprise e ON b.entreprise_id = e.id " +
           "INNER JOIN produit p ON vp.produit_id = p.id " +
           "WHERE e.id = :entrepriseId " +
           "AND (p.deleted IS NULL OR p.deleted = false) " +
           "GROUP BY vp.produit_id, p.nom " +
           "ORDER BY total_quantite DESC " +
           "LIMIT 3", nativeQuery = true)
    List<Object[]> findTop3ProduitsVendusByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Récupérer les 3 meilleurs produits vendus par entreprise avec filtre de période
    @Query(value = "SELECT vp.produit_id, p.nom, " +
           "SUM(CASE WHEN vp.est_remboursee = true AND vp.quantite_remboursee IS NOT NULL " +
           "THEN vp.quantite - vp.quantite_remboursee ELSE vp.quantite END) as total_quantite, " +
           "SUM(CASE WHEN vp.est_remboursee = true AND vp.montant_rembourse IS NOT NULL " +
           "THEN vp.montant_ligne - vp.montant_rembourse ELSE vp.montant_ligne END) as total_montant " +
           "FROM vente_produit vp " +
           "INNER JOIN vente v ON vp.vente_id = v.id " +
           "INNER JOIN boutique b ON v.boutique_id = b.id " +
           "INNER JOIN entreprise e ON b.entreprise_id = e.id " +
           "INNER JOIN produit p ON vp.produit_id = p.id " +
           "WHERE e.id = :entrepriseId " +
           "AND v.date_vente >= :dateDebut AND v.date_vente < :dateFin " +
           "AND (p.deleted IS NULL OR p.deleted = false) " +
           "GROUP BY vp.produit_id, p.nom " +
           "ORDER BY total_quantite DESC " +
           "LIMIT 3", nativeQuery = true)
    List<Object[]> findTop3ProduitsVendusByEntrepriseIdAndPeriode(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    // Nombre total d'articles vendus par entreprise et période
    @Query(value = "SELECT COALESCE(SUM(CASE WHEN vp.est_remboursee = true AND vp.quantite_remboursee IS NOT NULL " +
           "THEN vp.quantite - vp.quantite_remboursee ELSE vp.quantite END), 0) " +
           "FROM vente_produit vp " +
           "INNER JOIN vente v ON vp.vente_id = v.id " +
           "INNER JOIN boutique b ON v.boutique_id = b.id " +
           "WHERE b.entreprise_id = :entrepriseId " +
           "AND v.date_vente >= :dateDebut AND v.date_vente < :dateFin", nativeQuery = true)
    Long countTotalArticlesVendusByEntrepriseIdAndPeriode(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);
}