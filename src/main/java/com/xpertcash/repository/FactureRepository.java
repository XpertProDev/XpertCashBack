package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

   /** Pagination côté base : factures par entreprise (isolation multi-tenant). */
   @Query(value = "SELECT DISTINCT f FROM Facture f " +
          "LEFT JOIN FETCH f.boutique b " +
          "LEFT JOIN FETCH f.user u " +
          "LEFT JOIN FETCH f.fournisseur four " +
          "WHERE b.entreprise.id = :entrepriseId",
          countQuery = "SELECT COUNT(DISTINCT f) FROM Facture f JOIN f.boutique b JOIN b.entreprise e WHERE e.id = :entrepriseId")
   Page<Facture> findAllByEntrepriseIdPaginated(@Param("entrepriseId") Long entrepriseId, Pageable pageable);

   /** Pagination + recherche : factures par entreprise, filtre sur numeroFacture, type, description, codeFournisseur, nom fournisseur. */
   @Query(value = "SELECT DISTINCT f FROM Facture f " +
          "LEFT JOIN FETCH f.boutique b " +
          "LEFT JOIN FETCH f.user u " +
          "LEFT JOIN FETCH f.fournisseur four " +
          "WHERE b.entreprise.id = :entrepriseId " +
          "AND (LOWER(COALESCE(f.numeroFacture, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(f.type, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(f.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(f.codeFournisseur, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(four.nomSociete, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(four.nomComplet, '')) LIKE LOWER(CONCAT('%', :search, '%')))",
          countQuery = "SELECT COUNT(f) FROM Facture f JOIN f.boutique b JOIN b.entreprise e " +
          "LEFT JOIN f.fournisseur four " +
          "WHERE e.id = :entrepriseId " +
          "AND (LOWER(COALESCE(f.numeroFacture, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(f.type, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(f.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(f.codeFournisseur, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(four.nomSociete, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(four.nomComplet, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
   Page<Facture> findAllByEntrepriseIdPaginatedWithSearch(
           @Param("entrepriseId") Long entrepriseId,
           @Param("search") String search,
           Pageable pageable);

   /** Pagination côté base : factures par boutique et entreprise (isolation multi-tenant). */
   @Query(value = "SELECT DISTINCT f FROM Facture f " +
          "LEFT JOIN FETCH f.boutique b " +
          "LEFT JOIN FETCH f.user u " +
          "LEFT JOIN FETCH f.fournisseur four " +
          "WHERE b.id = :boutiqueId AND b.entreprise.id = :entrepriseId",
          countQuery = "SELECT COUNT(f) FROM Facture f JOIN f.boutique b JOIN b.entreprise e WHERE b.id = :boutiqueId AND e.id = :entrepriseId")
   Page<Facture> findByBoutiqueIdAndEntrepriseIdPaginated(
           @Param("boutiqueId") Long boutiqueId,
           @Param("entrepriseId") Long entrepriseId,
           Pageable pageable);

   /** Pagination + recherche : factures par boutique/entreprise, filtre sur numeroFacture, type, description, codeFournisseur, nom fournisseur. */
   @Query(value = "SELECT DISTINCT f FROM Facture f " +
          "LEFT JOIN FETCH f.boutique b " +
          "LEFT JOIN FETCH f.user u " +
          "LEFT JOIN FETCH f.fournisseur four " +
          "WHERE b.id = :boutiqueId AND b.entreprise.id = :entrepriseId " +
          "AND (LOWER(COALESCE(f.numeroFacture, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(f.type, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(f.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(f.codeFournisseur, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(four.nomSociete, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(four.nomComplet, '')) LIKE LOWER(CONCAT('%', :search, '%')))",
          countQuery = "SELECT COUNT(f) FROM Facture f JOIN f.boutique b JOIN b.entreprise e " +
          "LEFT JOIN f.fournisseur four " +
          "WHERE b.id = :boutiqueId AND e.id = :entrepriseId " +
          "AND (LOWER(COALESCE(f.numeroFacture, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(f.type, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(f.description, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(f.codeFournisseur, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(four.nomSociete, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
          "OR LOWER(COALESCE(four.nomComplet, '')) LIKE LOWER(CONCAT('%', :search, '%')))")
   Page<Facture> findByBoutiqueIdAndEntrepriseIdPaginatedWithSearch(
           @Param("boutiqueId") Long boutiqueId,
           @Param("entrepriseId") Long entrepriseId,
           @Param("search") String search,
           Pageable pageable);

}
