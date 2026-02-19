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

    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId")
    List<FactureReelle> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND MONTH(f.dateCreation) = :mois " +
           "AND YEAR(f.dateCreation) = :annee")
    List<FactureReelle> findByMonthAndYearAndEntreprise(
            @Param("mois") Integer mois, 
            @Param("annee") Integer annee, 
            @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND MONTH(f.dateCreation) = :mois")
    List<FactureReelle> findByMonthAndEntreprise(@Param("mois") Integer mois, @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND YEAR(f.dateCreation) = :annee")
    List<FactureReelle> findByYearAndEntreprise(@Param("annee") Integer annee, @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "ORDER BY f.dateCreationPro DESC")
    List<FactureReelle> findRecentFacturesReellesByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

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

    @Query("SELECT DISTINCT fr FROM FactureReelle fr " +
           "LEFT JOIN FETCH fr.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "ORDER BY fr.dateCreation DESC, fr.id DESC")
    Page<FactureReelle> findByEntrepriseOrderByDateCreationDescPaginated(
            @Param("entrepriseId") Long entrepriseId, 
            Pageable pageable);

    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND f.statutPaiement IN :statuts")
    List<FactureReelle> findByEntrepriseIdAndStatutPaiementIn(
            @Param("entrepriseId") Long entrepriseId, 
            @Param("statuts") List<StatutPaiementFacture> statuts);

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

    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH f.factureProForma fp " +
           "WHERE fp.id = :factureProFormaId " +
           "AND e.id = :entrepriseId")
    List<FactureReelle> findAllByFactureProFormaIdAndEntrepriseId(
            @Param("factureProFormaId") Long factureProFormaId,
            @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH f.utilisateurCreateur u " +
           "WHERE e.id = :entrepriseId " +
           "AND u.id = :utilisateurCreateurId " +
           "ORDER BY f.dateCreation DESC, f.id DESC")
    List<FactureReelle> findByEntrepriseAndUtilisateurCreateurOrderByDateCreationDesc(
            @Param("entrepriseId") Long entrepriseId,
            @Param("utilisateurCreateurId") Long utilisateurCreateurId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FactureReelle f " +
           "INNER JOIN f.entreprise e " +
           "WHERE e.id = :entrepriseId AND f.client.id = :clientId")
    boolean existsByClientIdAndEntrepriseId(@Param("clientId") Long clientId, @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FactureReelle f " +
           "INNER JOIN f.entreprise e " +
           "WHERE e.id = :entrepriseId AND f.entrepriseClient.id = :entrepriseClientId")
    boolean existsByEntrepriseClientIdAndEntrepriseId(@Param("entrepriseClientId") Long entrepriseClientId, @Param("entrepriseId") Long entrepriseId);

    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND FUNCTION('DATE', f.dateCreation) >= :start " +
           "AND FUNCTION('DATE', f.dateCreation) <= :end")
    List<FactureReelle> findByEntrepriseIdAndDateCreationBetween(
            @Param("entrepriseId") Long entrepriseId, 
            @Param("start") LocalDate start, 
            @Param("end") LocalDate end);

    @Query("SELECT COUNT(f) FROM FactureReelle f " +
           "INNER JOIN f.entreprise e " +
           "WHERE e.id = :entrepriseId")
    long countByEntrepriseId(@Param("entrepriseId") Long entrepriseId);


    // ==================== STATISTIQUES ====================

    // [0] statut, [1] count, [2] montant
    @Query(value = "SELECT f.statut_paiement, COUNT(f.id), COALESCE(SUM(f.total_facture), 0) " +
           "FROM facture_reelle f " +
           "WHERE f.entreprise_id = :entrepriseId " +
           "AND f.date_creation >= :dateDebut AND f.date_creation < :dateFin " +
           "GROUP BY f.statut_paiement", nativeQuery = true)
    List<Object[]> getStatistiquesParStatutByEntrepriseIdAndPeriode(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin);

    // [0] id, [1] nom, [2] type, [3] count, [4] montant
    @Query(value = "SELECT client_id, nom_client, type_client, nb_factures, montant_total FROM (" +
           "  SELECT f.client_id as client_id, c.nom_complet as nom_client, 'CLIENT' as type_client, " +
           "    COUNT(f.id) as nb_factures, COALESCE(SUM(f.total_facture), 0) as montant_total " +
           "  FROM facture_reelle f " +
           "  INNER JOIN client c ON f.client_id = c.id " +
           "  WHERE f.entreprise_id = :entrepriseId " +
           "    AND f.date_creation >= :dateDebut AND f.date_creation < :dateFin " +
           "    AND f.client_id IS NOT NULL " +
           "  GROUP BY f.client_id, c.nom_complet " +
           "  UNION ALL " +
           "  SELECT f.entreprise_client_id as client_id, ec.nom as nom_client, 'ENTREPRISE_CLIENT' as type_client, " +
           "    COUNT(f.id) as nb_factures, COALESCE(SUM(f.total_facture), 0) as montant_total " +
           "  FROM facture_reelle f " +
           "  INNER JOIN entreprise_client ec ON f.entreprise_client_id = ec.id " +
           "  WHERE f.entreprise_id = :entrepriseId " +
           "    AND f.date_creation >= :dateDebut AND f.date_creation < :dateFin " +
           "    AND f.entreprise_client_id IS NOT NULL " +
           "  GROUP BY f.entreprise_client_id, ec.nom " +
           ") AS combined ORDER BY montant_total DESC", nativeQuery = true)
    List<Object[]> findAllTopClientsByEntrepriseIdAndPeriode(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin);

    // [0] id, [1] nom, [2] count, [3] montant
    @Query(value = "SELECT f.utilisateur_createur_id, u.nom_complet, COUNT(f.id) as nb_factures, " +
           "COALESCE(SUM(f.total_facture), 0) as montant_total " +
           "FROM facture_reelle f " +
           "INNER JOIN user u ON f.utilisateur_createur_id = u.id " +
           "WHERE f.entreprise_id = :entrepriseId " +
           "AND f.date_creation >= :dateDebut AND f.date_creation < :dateFin " +
           "AND f.utilisateur_createur_id IS NOT NULL " +
           "GROUP BY f.utilisateur_createur_id, u.nom_complet " +
           "ORDER BY montant_total DESC", nativeQuery = true)
    List<Object[]> findTopCreateursByEntrepriseIdAndPeriode(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDate dateDebut,
            @Param("dateFin") LocalDate dateFin);

    /** Pour pagination dettes détaillées : charger factures par IDs avec client / entrepriseClient. */
    @Query("SELECT DISTINCT f FROM FactureReelle f " +
           "LEFT JOIN FETCH f.client " +
           "LEFT JOIN FETCH f.entrepriseClient " +
           "WHERE f.id IN :ids")
    List<FactureReelle> findByIdInWithDetailsForDettes(@Param("ids") List<Long> ids);

}
