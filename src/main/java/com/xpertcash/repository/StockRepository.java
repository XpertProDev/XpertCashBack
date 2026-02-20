package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;

import jakarta.persistence.LockModeType;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    
    // Récupérer tous les stocks d'une boutique (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT s FROM Stock s " +
           "LEFT JOIN FETCH s.boutique b " +
           "LEFT JOIN FETCH s.produit p " +
           "WHERE b.id = :boutiqueId")
    List<Stock> findByBoutiqueId(@Param("boutiqueId") Long boutiqueId);

    //  ATTENTION: Cette méthode ne filtre PAS par boutique - peut retourner plusieurs stocks
    // Un produit peut avoir un stock dans plusieurs boutiques
    // Préférer findByBoutiqueAndProduit ou findByProduitIdAndBoutiqueId
    Stock findByProduit(Produit produit);
    
    // Recherche par produit ID et boutique ID (pour isolation)
    @Query("SELECT s FROM Stock s " +
           "LEFT JOIN FETCH s.boutique b " +
           "LEFT JOIN FETCH s.produit p " +
           "WHERE p.id = :produitId AND b.id = :boutiqueId")
    Stock findByProduitIdAndBoutiqueId(@Param("produitId") Long produitId, @Param("boutiqueId") Long boutiqueId);

    // Recherche par boutique et produit (optimisé avec JOIN FETCH)
    @Query("SELECT s FROM Stock s " +
           "LEFT JOIN FETCH s.boutique b " +
           "LEFT JOIN FETCH s.produit p " +
           "WHERE b.id = :boutiqueId AND p.id = :produitId")
    Stock findByBoutiqueAndProduit(@Param("boutiqueId") Long boutiqueId, @Param("produitId") Long produitId);
    
    // Recherche par boutique et stock actuel supérieur (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT s FROM Stock s " +
           "LEFT JOIN FETCH s.boutique b " +
           "LEFT JOIN FETCH s.produit p " +
           "WHERE b.id = :boutiqueId AND s.stockActuel > :quantite")
    List<Stock> findByBoutiqueAndStockActuelGreaterThan(@Param("boutiqueId") Long boutiqueId, @Param("quantite") Integer quantite);

    //  Récupère tous les stocks liés à une liste de produits pour une boutique spécifique
    // en posant un verrou PESSIMISTIC_WRITE (bloque la ligne pendant la transaction)
    @Query("SELECT s FROM Stock s " +
           "LEFT JOIN FETCH s.boutique b " +
           "LEFT JOIN FETCH s.produit p " +
           "WHERE p.id IN :produitIds AND b.id = :boutiqueId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Stock> findAllByProduitIdInAndBoutiqueIdWithLock(@Param("produitIds") List<Long> produitIds, @Param("boutiqueId") Long boutiqueId);

    //  Récupère tous les stocks liés à une liste de produits (gardé pour compatibilité, mais moins optimal)
    // en posant un verrou PESSIMISTIC_WRITE (bloque la ligne pendant la transaction)
    @Query("SELECT s FROM Stock s WHERE s.produit.id IN :ids")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Stock> findAllByProduitIdInWithLock(@Param("ids") List<Long> ids);

    // Récupérer tous les stocks d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT s FROM Stock s " +
           "LEFT JOIN FETCH s.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "LEFT JOIN FETCH s.produit p " +
           "WHERE e.id = :entrepriseId")
    List<Stock> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Compter tous les enregistrements de stock pour une entreprise (optimisé avec JOIN)
    @Query("SELECT COUNT(s) FROM Stock s " +
           "INNER JOIN s.boutique b " +
           "INNER JOIN b.entreprise e " +
           "WHERE e.id = :entrepriseId")
    long countByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    /** Pagination côté base : stocks par entreprise (isolation multi-tenant). */
    @Query(value = "SELECT DISTINCT s FROM Stock s " +
            "LEFT JOIN FETCH s.boutique b " +
            "LEFT JOIN FETCH s.produit p " +
            "WHERE b.entreprise.id = :entrepriseId",
            countQuery = "SELECT COUNT(DISTINCT s) FROM Stock s INNER JOIN s.boutique b INNER JOIN b.entreprise e WHERE e.id = :entrepriseId")
    Page<Stock> findByEntrepriseIdPaginated(@Param("entrepriseId") Long entrepriseId, Pageable pageable);
}

