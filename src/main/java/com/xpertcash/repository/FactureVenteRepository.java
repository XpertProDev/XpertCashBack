package com.xpertcash.repository;

import com.xpertcash.entity.FactureVente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactureVenteRepository extends JpaRepository<FactureVente, Long> {

    // Récupérer toutes les factures de vente d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureVente f " +
           "LEFT JOIN FETCH f.vente v " +
           "LEFT JOIN FETCH v.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE e.id = :entrepriseId")
    List<FactureVente> findAllByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Récupérer les factures de vente d'une entreprise filtrées par période (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureVente f " +
           "LEFT JOIN FETCH f.vente v " +
           "LEFT JOIN FETCH v.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND f.dateEmission >= :dateDebut " +
           "AND f.dateEmission < :dateFin")
    List<FactureVente> findAllByEntrepriseIdAndDateEmissionBetween(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    /** Pagination côté base avec filtres optionnels (période, vendeur, boutique). Relations vente/boutique/vendeur/caisse chargées (évite N+1). */
    @Query(value = "SELECT DISTINCT f FROM FactureVente f " +
           "LEFT JOIN FETCH f.vente v " +
           "LEFT JOIN FETCH v.boutique b " +
           "LEFT JOIN FETCH v.vendeur " +
           "LEFT JOIN FETCH v.caisse " +
           "LEFT JOIN b.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND (:dateDebut IS NULL OR f.dateEmission >= :dateDebut) " +
           "AND (:dateFin IS NULL OR f.dateEmission < :dateFin) " +
           "AND (:vendeurId IS NULL OR v.vendeur.id = :vendeurId) " +
           "AND (:boutiqueId IS NULL OR b.id = :boutiqueId)",
           countQuery = "SELECT COUNT(DISTINCT f) FROM FactureVente f " +
           "LEFT JOIN f.vente v " +
           "LEFT JOIN v.boutique b " +
           "LEFT JOIN b.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND (:dateDebut IS NULL OR f.dateEmission >= :dateDebut) " +
           "AND (:dateFin IS NULL OR f.dateEmission < :dateFin) " +
           "AND (:vendeurId IS NULL OR v.vendeur.id = :vendeurId) " +
           "AND (:boutiqueId IS NULL OR b.id = :boutiqueId)")
    Page<FactureVente> findAllPaginatedWithFilters(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("vendeurId") Long vendeurId,
            @Param("boutiqueId") Long boutiqueId,
            Pageable pageable);

    /** Pagination + recherche par numéro facture, nom vendeur ou nom client (côté base). */
    @Query(value = "SELECT DISTINCT f FROM FactureVente f " +
           "LEFT JOIN FETCH f.vente v " +
           "LEFT JOIN FETCH v.boutique b " +
           "LEFT JOIN FETCH v.vendeur " +
           "LEFT JOIN FETCH v.caisse " +
           "LEFT JOIN b.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND (:dateDebut IS NULL OR f.dateEmission >= :dateDebut) " +
           "AND (:dateFin IS NULL OR f.dateEmission < :dateFin) " +
           "AND (:vendeurId IS NULL OR v.vendeur.id = :vendeurId) " +
           "AND (:boutiqueId IS NULL OR b.id = :boutiqueId) " +
           "AND (LOWER(COALESCE(f.numeroFacture, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')) " +
           "     OR (v.vendeur IS NOT NULL AND LOWER(COALESCE(v.vendeur.nomComplet, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
           "     OR (v.client IS NOT NULL AND LOWER(COALESCE(v.client.nomComplet, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
           "     OR (v.entrepriseClient IS NOT NULL AND LOWER(COALESCE(v.entrepriseClient.nom, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))))",
           countQuery = "SELECT COUNT(DISTINCT f) FROM FactureVente f " +
           "LEFT JOIN f.vente v " +
           "LEFT JOIN v.boutique b " +
           "LEFT JOIN b.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND (:dateDebut IS NULL OR f.dateEmission >= :dateDebut) " +
           "AND (:dateFin IS NULL OR f.dateEmission < :dateFin) " +
           "AND (:vendeurId IS NULL OR v.vendeur.id = :vendeurId) " +
           "AND (:boutiqueId IS NULL OR b.id = :boutiqueId) " +
           "AND (LOWER(COALESCE(f.numeroFacture, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')) " +
           "     OR (v.vendeur IS NOT NULL AND LOWER(COALESCE(v.vendeur.nomComplet, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
           "     OR (v.client IS NOT NULL AND LOWER(COALESCE(v.client.nomComplet, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
           "     OR (v.entrepriseClient IS NOT NULL AND LOWER(COALESCE(v.entrepriseClient.nom, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))))")
    Page<FactureVente> findAllPaginatedWithFiltersAndSearch(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("vendeurId") Long vendeurId,
            @Param("boutiqueId") Long boutiqueId,
            @Param("search") String search,
            Pageable pageable);

    /** Somme des montants totaux pour les factures correspondant aux mêmes filtres (pour stats globales). */
    @Query("SELECT COALESCE(SUM(f.montantTotal), 0) FROM FactureVente f " +
           "LEFT JOIN f.vente v " +
           "LEFT JOIN v.boutique b " +
           "LEFT JOIN b.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND (:dateDebut IS NULL OR f.dateEmission >= :dateDebut) " +
           "AND (:dateFin IS NULL OR f.dateEmission < :dateFin) " +
           "AND (:vendeurId IS NULL OR v.vendeur.id = :vendeurId) " +
           "AND (:boutiqueId IS NULL OR b.id = :boutiqueId)")
    double sumMontantTotalWithFilters(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("vendeurId") Long vendeurId,
            @Param("boutiqueId") Long boutiqueId);

    /** Somme des montants avec le même filtre + recherche (pour stats quand search est actif). */
    @Query("SELECT COALESCE(SUM(f.montantTotal), 0) FROM FactureVente f " +
           "LEFT JOIN f.vente v " +
           "LEFT JOIN v.boutique b " +
           "LEFT JOIN b.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND (:dateDebut IS NULL OR f.dateEmission >= :dateDebut) " +
           "AND (:dateFin IS NULL OR f.dateEmission < :dateFin) " +
           "AND (:vendeurId IS NULL OR v.vendeur.id = :vendeurId) " +
           "AND (:boutiqueId IS NULL OR b.id = :boutiqueId) " +
           "AND (LOWER(COALESCE(f.numeroFacture, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')) " +
           "     OR (v.vendeur IS NOT NULL AND LOWER(COALESCE(v.vendeur.nomComplet, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
           "     OR (v.client IS NOT NULL AND LOWER(COALESCE(v.client.nomComplet, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
           "     OR (v.entrepriseClient IS NOT NULL AND LOWER(COALESCE(v.entrepriseClient.nom, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))))")
    double sumMontantTotalWithFiltersAndSearch(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("vendeurId") Long vendeurId,
            @Param("boutiqueId") Long boutiqueId,
            @Param("search") String search);

    // Recherche par vente et entreprise (pour isolation)
    @Query("SELECT DISTINCT f FROM FactureVente f " +
           "LEFT JOIN FETCH f.vente v " +
           "LEFT JOIN FETCH v.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE v.id = :venteId " +
           "AND e.id = :entrepriseId")
    Optional<FactureVente> findByVenteIdAndEntrepriseId(
            @Param("venteId") Long venteId,
            @Param("entrepriseId") Long entrepriseId);

    /** Charge les factures vente pour une liste de ventes et une entreprise (évite N+1 dans getDettesPos). */
    @Query("SELECT f FROM FactureVente f LEFT JOIN FETCH f.vente v " +
           "WHERE v.id IN :venteIds AND v.boutique.entreprise.id = :entrepriseId")
    List<FactureVente> findByVenteIdInAndEntrepriseId(
            @Param("venteIds") List<Long> venteIds,
            @Param("entrepriseId") Long entrepriseId);

}