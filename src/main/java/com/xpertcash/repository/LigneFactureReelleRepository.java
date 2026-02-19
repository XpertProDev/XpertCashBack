package com.xpertcash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.LigneFactureReelle;

import java.util.List;

@Repository
public interface LigneFactureReelleRepository extends JpaRepository<LigneFactureReelle, Long>{

    /** Charge toutes les lignes pour une liste de factures réelles (évite N+1 en liste paginée). */
    @Query("SELECT l FROM LigneFactureReelle l WHERE l.factureReelle.id IN :factureReelleIds")
    List<LigneFactureReelle> findByFactureReelleIdIn(@Param("factureReelleIds") List<Long> factureReelleIds);

    // Vérifier si un produit est utilisé dans des factures réelles d'une entreprise (pour isolation)
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END " +
           "FROM LigneFactureReelle l " +
           "INNER JOIN l.factureReelle f " +
           "INNER JOIN f.entreprise e " +
           "WHERE l.produit.id = :produitId AND e.id = :entrepriseId")
    boolean existsByProduitIdAndEntrepriseId(
            @Param("produitId") Long produitId,
            @Param("entrepriseId") Long entrepriseId);

}
