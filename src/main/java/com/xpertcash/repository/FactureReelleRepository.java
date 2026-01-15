package com.xpertcash.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.FactureReelle;
import com.xpertcash.entity.Enum.StatutPaiementFacture;

@Repository
public interface FactureReelleRepository extends JpaRepository<FactureReelle, Long> {

    // Recherche de la dernière facture réelle par date et entreprise (pour isolation)
    @Query("SELECT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND FUNCTION('DATE', f.dateCreation) = :dateCreation " +
           "ORDER BY f.numeroFacture DESC")
    Optional<FactureReelle> findTopByDateCreationAndEntrepriseIdOrderByNumeroFactureDesc(
            @Param("dateCreation") LocalDate dateCreation,
            @Param("entrepriseId") Long entrepriseId);
    
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND FUNCTION('YEAR', f.dateCreation) = :year " +
           "ORDER BY f.numeroFacture DESC")
    List<FactureReelle> findFacturesDeLAnneeParEntreprise(@Param("entrepriseId") Long entrepriseId, @Param("year") int year);
    
    // Récupérer les factures par entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId")
    List<FactureReelle> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Tri par mois et année (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND MONTH(f.dateCreation) = :mois " +
           "AND YEAR(f.dateCreation) = :annee")
    List<FactureReelle> findByMonthAndYearAndEntreprise(
            @Param("mois") Integer mois, 
            @Param("annee") Integer annee, 
            @Param("entrepriseId") Long entrepriseId);
    
    // Tri par mois (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND MONTH(f.dateCreation) = :mois")
    List<FactureReelle> findByMonthAndEntreprise(@Param("mois") Integer mois, @Param("entrepriseId") Long entrepriseId);
    
    // Tri par année (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND YEAR(f.dateCreation) = :annee")
    List<FactureReelle> findByYearAndEntreprise(@Param("annee") Integer annee, @Param("entrepriseId") Long entrepriseId);
    
    // Récupère les factures réelles récentes d'une entreprise (triées par date de création décroissante, optimisé)
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "ORDER BY f.dateCreationPro DESC")
    List<FactureReelle> findRecentFacturesReellesByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Recherche par facture proforma et entreprise (pour isolation)
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH f.factureProForma fp " +
           "WHERE fp.id = :factureProFormaId " +
           "AND e.id = :entrepriseId")
    Optional<FactureReelle> findByFactureProFormaIdAndEntrepriseId(
            @Param("factureProFormaId") Long factureProFormaId,
            @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT fr FROM FactureReelle fr " +
           "LEFT JOIN FETCH fr.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "ORDER BY fr.dateCreation DESC, fr.id DESC")
    List<FactureReelle> findByEntrepriseOrderByDateCreationDesc(@Param("entrepriseId") Long entrepriseId);

    // Méthode paginée pour récupérer les factures par entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT fr FROM FactureReelle fr " +
           "LEFT JOIN FETCH fr.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "ORDER BY fr.dateCreation DESC, fr.id DESC")
    Page<FactureReelle> findByEntrepriseOrderByDateCreationDescPaginated(
            @Param("entrepriseId") Long entrepriseId, 
            Pageable pageable);


    // Recherche par entreprise et statuts de paiement (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND f.statutPaiement IN :statuts")
    List<FactureReelle> findByEntrepriseIdAndStatutPaiementIn(
            @Param("entrepriseId") Long entrepriseId, 
            @Param("statuts") List<StatutPaiementFacture> statuts);

    // Recherche par entreprise, utilisateur et statuts de paiement (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH f.utilisateurCreateur u " +
           "WHERE e.id = :entrepriseId " +
           "AND u.id = :utilisateurId " +
           "AND f.statutPaiement IN :statuts")
    List<FactureReelle> findByEntrepriseIdAndUtilisateurCreateurIdAndStatutPaiementIn(
            @Param("entrepriseId") Long entrepriseId, 
            @Param("utilisateurId") Long utilisateurId, 
            @Param("statuts") List<StatutPaiementFacture> statuts);

    // Recherche toutes les factures réelles par facture proforma et entreprise (pour isolation)
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH f.factureProForma fp " +
           "WHERE fp.id = :factureProFormaId " +
           "AND e.id = :entrepriseId")
    List<FactureReelle> findAllByFactureProFormaIdAndEntrepriseId(
            @Param("factureProFormaId") Long factureProFormaId,
            @Param("entrepriseId") Long entrepriseId);

    // Recherche par entreprise et utilisateur créateur (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH f.utilisateurCreateur u " +
           "WHERE e.id = :entrepriseId " +
           "AND u.id = :utilisateurCreateurId " +
           "ORDER BY f.dateCreation DESC, f.id DESC")
    List<FactureReelle> findByEntrepriseAndUtilisateurCreateurOrderByDateCreationDesc(
            @Param("entrepriseId") Long entrepriseId,
            @Param("utilisateurCreateurId") Long utilisateurCreateurId);

    // Vérifier l'existence par client et entreprise (pour isolation)
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FactureReelle f " +
           "INNER JOIN f.entreprise e " +
           "WHERE e.id = :entrepriseId AND f.client.id = :clientId")
    boolean existsByClientIdAndEntrepriseId(@Param("clientId") Long clientId, @Param("entrepriseId") Long entrepriseId);

    // Vérifier l'existence par entreprise client et entreprise (pour isolation)
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FactureReelle f " +
           "INNER JOIN f.entreprise e " +
           "WHERE e.id = :entrepriseId AND f.entrepriseClient.id = :entrepriseClientId")
    boolean existsByEntrepriseClientIdAndEntrepriseId(@Param("entrepriseClientId") Long entrepriseClientId, @Param("entrepriseId") Long entrepriseId);

    // Récupérer les factures par entreprise et période (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND FUNCTION('DATE', f.dateCreation) >= :start " +
           "AND FUNCTION('DATE', f.dateCreation) <= :end")
    List<FactureReelle> findByEntrepriseIdAndDateCreationBetween(
            @Param("entrepriseId") Long entrepriseId, 
            @Param("start") LocalDate start, 
            @Param("end") LocalDate end);

    // Compter toutes les factures réelles d'une entreprise (optimisé avec INNER JOIN)
    @Query("SELECT COUNT(f) FROM FactureReelle f " +
           "INNER JOIN f.entreprise e " +
           "WHERE e.id = :entrepriseId")
    long countByEntrepriseId(@Param("entrepriseId") Long entrepriseId);






}

