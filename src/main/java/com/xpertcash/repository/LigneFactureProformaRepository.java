package com.xpertcash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.LigneFactureProforma;

@Repository
public interface LigneFactureProformaRepository extends JpaRepository<LigneFactureProforma, Long> {
    
    // Vérifier si un produit est utilisé dans des factures proforma d'une entreprise (pour isolation)
    @Query("SELECT CASE WHEN COUNT(l) > 0 THEN true ELSE false END " +
           "FROM LigneFactureProforma l " +
           "INNER JOIN l.factureProForma f " +
           "INNER JOIN f.entreprise e " +
           "WHERE l.produit.id = :produitId AND e.id = :entrepriseId")
    boolean existsByProduitIdAndEntrepriseId(
            @Param("produitId") Long produitId,
            @Param("entrepriseId") Long entrepriseId);

    // Supprimer les lignes d'une facture proforma par ID et entreprise (pour isolation)
    @Modifying
    @Query("DELETE FROM LigneFactureProforma l " +
           "WHERE l.factureProForma.id = :factureProFormaId " +
           "AND l.factureProForma.entreprise.id = :entrepriseId")
    void deleteByFactureProFormaIdAndEntrepriseId(
            @Param("factureProFormaId") Long factureProFormaId,
            @Param("entrepriseId") Long entrepriseId);

}
