package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Facture;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long>{
   
   // Recherche par boutique et entreprise (pour isolation)
   @Query("SELECT DISTINCT f FROM Facture f " +
          "LEFT JOIN FETCH f.boutique b " +
          "LEFT JOIN FETCH b.entreprise e " +
          "WHERE b.id = :boutiqueId " +
          "AND e.id = :entrepriseId")
   List<Facture> findByBoutiqueIdAndEntrepriseId(
           @Param("boutiqueId") Long boutiqueId,
           @Param("entrepriseId") Long entrepriseId);

   // Recherche par fournisseur et entreprise (pour isolation)
   @Query("SELECT DISTINCT f FROM Facture f " +
          "LEFT JOIN FETCH f.fournisseur four " +
          "LEFT JOIN FETCH f.boutique b " +
          "LEFT JOIN FETCH b.entreprise e " +
          "WHERE four.id = :fournisseurId " +
          "AND e.id = :entrepriseId")
   List<Facture> findByFournisseurIdAndEntrepriseId(
           @Param("fournisseurId") Long fournisseurId,
           @Param("entrepriseId") Long entrepriseId);

   @Query("SELECT DISTINCT f FROM Facture f " +
          "LEFT JOIN FETCH f.boutique b " +
          "LEFT JOIN FETCH b.entreprise e " +
          "WHERE e.id = :entrepriseId " +
          "AND YEAR(f.dateFacture) = :year")
   List<Facture> findByYearAndEntrepriseId(@Param("year") int year, @Param("entrepriseId") Long entrepriseId);

   // Vérifier l'existence par fournisseur et entreprise (pour isolation)
   @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Facture f " +
          "INNER JOIN f.fournisseur four " +
          "INNER JOIN f.boutique b " +
          "INNER JOIN b.entreprise e " +
          "WHERE four.id = :fournisseurId " +
          "AND e.id = :entrepriseId")
   boolean existsByFournisseurIdAndEntrepriseId(
           @Param("fournisseurId") Long fournisseurId,
           @Param("entrepriseId") Long entrepriseId);
 
   // Récupérer toutes les factures d'une entreprise (optimisé avec JOIN FETCH)
   @Query("SELECT DISTINCT f FROM Facture f " +
          "LEFT JOIN FETCH f.boutique b " +
          "LEFT JOIN FETCH b.entreprise e " +
          "LEFT JOIN FETCH f.fournisseur four " +
          "LEFT JOIN FETCH f.user u " +
          "WHERE e.id = :entrepriseId")
   List<Facture> findAllByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

}
