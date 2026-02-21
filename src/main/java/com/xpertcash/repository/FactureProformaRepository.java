package com.xpertcash.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.User;
import com.xpertcash.entity.Enum.StatutFactureProForma;

@Repository
public interface FactureProformaRepository extends JpaRepository<FactureProForma, Long> {
    
    @Query("SELECT f FROM FactureProForma f LEFT JOIN FETCH f.client WHERE f.id = :id")
    Optional<FactureProForma> findByIdWithClient(@Param("id") Long id);

    // Méthode optimisée pour charger une facture avec toutes les relations nécessaires incluant les approbateurs
    // Note: On évite de FETCH plusieurs collections en même temps pour éviter le produit cartésien
    // Les lignesFacture seront chargées via la relation EAGER ou séparément si nécessaire
    @Query("SELECT DISTINCT f FROM FactureProForma f " +
           "LEFT JOIN FETCH f.approbateurs " +
           "LEFT JOIN FETCH f.utilisateurCreateur " +
           "LEFT JOIN FETCH f.utilisateurModificateur " +
           "LEFT JOIN FETCH f.utilisateurValidateur " +
           "LEFT JOIN FETCH f.utilisateurApprobateur " +
           "LEFT JOIN FETCH f.client " +
           "LEFT JOIN FETCH f.entrepriseClient " +
           "LEFT JOIN FETCH f.entreprise " +
           "WHERE f.id = :id")
    Optional<FactureProForma> findByIdWithRelations(@Param("id") Long id);

    // Recherche de factures existantes par entreprise (pour isolation)
    @Query("SELECT f FROM FactureProForma f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND f.statut = :statut " +
           "AND (:clientId IS NULL OR f.client.id = :clientId) " +
           "AND (:entrepriseClientId IS NULL OR f.entrepriseClient.id = :entrepriseClientId)")
    List<FactureProForma> findExistingFacturesByEntrepriseId(@Param("entrepriseId") Long entrepriseId,
                                                             @Param("clientId") Long clientId, 
                                                             @Param("entrepriseClientId") Long entrepriseClientId,
                                                             @Param("statut") StatutFactureProForma statut);

    // Recherche de la dernière facture par date et entreprise (pour isolation)
    @Query("SELECT f FROM FactureProForma f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND FUNCTION('DATE', f.dateCreation) = :dateCreation " +
           "ORDER BY f.numeroFacture DESC")
    Optional<FactureProForma> findTopByDateCreationAndEntrepriseIdOrderByNumeroFactureDesc(
            @Param("dateCreation") LocalDate dateCreation,
            @Param("entrepriseId") Long entrepriseId);

     // Trouver les factures à relancer par entreprise (pour isolation)
    @Query("SELECT DISTINCT f FROM FactureProForma f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND f.dateRelance < :now " +
           "AND (f.dernierRappelEnvoye IS NULL OR f.dernierRappelEnvoye < :now)")
    List<FactureProForma> findByDateRelanceBeforeAndDernierRappelEnvoyeIsNullOrDernierRappelEnvoyeBeforeByEntrepriseId(
            @Param("entrepriseId") Long entrepriseId,
            @Param("now") LocalDateTime now);

    // Trouver les factures à envoyer par entreprise (pour isolation)
    @Query("SELECT DISTINCT f FROM FactureProForma f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND f.dateRelance <= :now " +
           "AND f.dernierRappelEnvoye IS NULL " +
           "AND f.notifie = false")
    List<FactureProForma> findFacturesAEnvoyerByEntrepriseId(@Param("entrepriseId") Long entrepriseId, @Param("now") LocalDateTime now);

    @Query("SELECT f FROM FactureProForma f WHERE f.entreprise.id = :entrepriseId AND FUNCTION('YEAR', f.dateCreation) = :year ORDER BY f.numeroFacture DESC")
    List<FactureProForma> findFacturesDeLAnneeParEntreprise(@Param("entrepriseId") Long entrepriseId, @Param("year") int year);

    // Récupérer toutes les factures d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureProForma f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH f.client c " +
           "LEFT JOIN FETCH f.entrepriseClient ec " +
           "WHERE e.id = :entrepriseId " +
           "ORDER BY f.dateCreation DESC, f.id DESC")
    List<FactureProForma> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    @Query("""
    SELECT f FROM FactureProForma f
    WHERE f.entreprise.id = :entrepriseId
      AND (
          f.utilisateurCreateur.id = :userId
          OR f.utilisateurApprobateur.id = :userId
          OR :userId IN (SELECT u.id FROM f.approbateurs u)
      )
    ORDER BY f.dateCreation DESC, f.id DESC
    """)
    List<FactureProForma> findByEntrepriseIdAndUtilisateur(@Param("userId") Long userId, @Param("entrepriseId") Long entrepriseId);

    // Recherche par client ou entreprise client et entreprise (pour isolation)
    @Query("SELECT DISTINCT f FROM FactureProForma f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH f.client c " +
           "LEFT JOIN FETCH f.entrepriseClient ec " +
           "WHERE e.id = :entrepriseId " +
           "AND ((:clientId IS NOT NULL AND c.id = :clientId) OR " +
           "     (:entrepriseClientId IS NOT NULL AND ec.id = :entrepriseClientId))")
    List<FactureProForma> findByClientIdOrEntrepriseClientIdAndEntrepriseId(
            @Param("entrepriseId") Long entrepriseId,
            @Param("clientId") Long clientId,
            @Param("entrepriseClientId") Long entrepriseClientId);

    // Vérifier l'existence par client et entreprise (pour isolation)
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FactureProForma f " +
           "INNER JOIN f.entreprise e " +
           "WHERE e.id = :entrepriseId AND f.client.id = :clientId")
    boolean existsByClientIdAndEntrepriseId(@Param("clientId") Long clientId, @Param("entrepriseId") Long entrepriseId);

    // Vérifier l'existence par entreprise client et entreprise (pour isolation)
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FactureProForma f " +
           "INNER JOIN f.entreprise e " +
           "WHERE e.id = :entrepriseId AND f.entrepriseClient.id = :entrepriseClientId")
    boolean existsByEntrepriseClientIdAndEntrepriseId(@Param("entrepriseClientId") Long entrepriseClientId, @Param("entrepriseId") Long entrepriseId);

    // Récupérer les factures par entreprise et période (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT f FROM FactureProForma f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH f.client c " +
           "LEFT JOIN FETCH f.entrepriseClient ec " +
           "WHERE e.id = :entrepriseId " +
           "AND f.dateCreation >= :start AND f.dateCreation <= :end")
    List<FactureProForma> findByEntrepriseIdAndDateCreationBetween(
            @Param("entrepriseId") Long entrepriseId, 
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end);

    // Recherche par utilisateur créateur et entreprise (pour isolation)
    @Query("SELECT DISTINCT f FROM FactureProForma f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH f.utilisateurCreateur u " +
           "WHERE e.id = :entrepriseId AND u.id = :userId")
    List<FactureProForma> findByUtilisateurCreateurIdAndEntrepriseId(
            @Param("userId") Long userId,
            @Param("entrepriseId") Long entrepriseId);


     boolean existsByApprobateursAndEntrepriseId(User approbateur, Long entrepriseId);


     // Récupérer toutes les factures d'une entreprise avec les relations nécessaires en une seule requête
@Query("SELECT DISTINCT f FROM FactureProForma f " +
       "LEFT JOIN FETCH f.approbateurs a " +
       "LEFT JOIN FETCH f.utilisateurCreateur u " +
       "LEFT JOIN FETCH f.client c " +
       "LEFT JOIN FETCH f.entrepriseClient ec " +
       "LEFT JOIN FETCH f.entreprise e " +
       "WHERE f.entreprise.id = :entrepriseId")
List<FactureProForma> findFacturesAvecRelationsParEntreprise(@Param("entrepriseId") Long entrepriseId);


// Récupérer toutes les factures d'une entreprise dans une période avec les relations nécessaires
@Query("SELECT f FROM FactureProForma f " +
       "LEFT JOIN FETCH f.utilisateurCreateur u " +
       "LEFT JOIN FETCH f.approbateurs a " +
       "LEFT JOIN FETCH f.client c " +
       "LEFT JOIN FETCH f.entrepriseClient ec " +
       "WHERE f.entreprise.id = :entrepriseId " +
       "AND f.dateCreation >= :dateStart AND f.dateCreation < :dateEnd")
List<FactureProForma> findFacturesAvecRelationsParEntrepriseEtPeriode(
        @Param("entrepriseId") Long entrepriseId,
        @Param("dateStart") LocalDateTime dateStart,
        @Param("dateEnd") LocalDateTime dateEnd
);

// Méthodes de pagination pour les factures proforma
@Query("SELECT f FROM FactureProForma f " +
       "LEFT JOIN FETCH f.approbateurs a " +
       "LEFT JOIN FETCH f.utilisateurCreateur u " +
       "LEFT JOIN FETCH f.client c " +
       "LEFT JOIN FETCH f.entrepriseClient ec " +
       "LEFT JOIN FETCH f.entreprise e " +
       "WHERE f.entreprise.id = :entrepriseId " +
       "ORDER BY f.dateCreation DESC, f.id DESC")
Page<FactureProForma> findFacturesAvecRelationsParEntreprisePaginated(
    @Param("entrepriseId") Long entrepriseId,
    Pageable pageable);

// Pagination avec recherche par numéro ou nom client (personne ou entreprise)
@Query("SELECT f FROM FactureProForma f " +
       "LEFT JOIN FETCH f.approbateurs a " +
       "LEFT JOIN FETCH f.utilisateurCreateur u " +
       "LEFT JOIN FETCH f.client c " +
       "LEFT JOIN FETCH f.entrepriseClient ec " +
       "LEFT JOIN FETCH f.entreprise e " +
       "WHERE f.entreprise.id = :entrepriseId " +
       "AND (LOWER(COALESCE(f.numeroFacture, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')) " +
       "     OR (f.client IS NOT NULL AND LOWER(COALESCE(f.client.nomComplet, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
       "     OR (f.entrepriseClient IS NOT NULL AND LOWER(COALESCE(f.entrepriseClient.nom, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')))) " +
       "ORDER BY f.dateCreation DESC, f.id DESC")
Page<FactureProForma> findFacturesAvecRelationsParEntreprisePaginatedWithSearch(
    @Param("entrepriseId") Long entrepriseId,
    @Param("search") String search,
    Pageable pageable);

// Méthode de pagination avec filtrage par utilisateur (créateur ou approbateur)
@Query("SELECT DISTINCT f FROM FactureProForma f " +
       "LEFT JOIN FETCH f.approbateurs a " +
       "LEFT JOIN FETCH f.utilisateurCreateur u " +
       "LEFT JOIN FETCH f.client c " +
       "LEFT JOIN FETCH f.entrepriseClient ec " +
       "LEFT JOIN FETCH f.entreprise e " +
       "WHERE f.entreprise.id = :entrepriseId " +
       "AND (f.utilisateurCreateur.id = :userId " +
       "     OR f.utilisateurApprobateur.id = :userId " +
       "     OR :userId IN (SELECT u2.id FROM f.approbateurs u2)) " +
       "ORDER BY f.dateCreation DESC, f.id DESC")
Page<FactureProForma> findFacturesAvecRelationsParEntrepriseEtUtilisateurPaginated(
    @Param("entrepriseId") Long entrepriseId,
    @Param("userId") Long userId,
    Pageable pageable);

@Query("SELECT DISTINCT f FROM FactureProForma f " +
       "LEFT JOIN FETCH f.approbateurs a " +
       "LEFT JOIN FETCH f.utilisateurCreateur u " +
       "LEFT JOIN FETCH f.client c " +
       "LEFT JOIN FETCH f.entrepriseClient ec " +
       "LEFT JOIN FETCH f.entreprise e " +
       "WHERE f.entreprise.id = :entrepriseId " +
       "AND (f.utilisateurCreateur.id = :userId " +
       "     OR f.utilisateurApprobateur.id = :userId " +
       "     OR :userId IN (SELECT u2.id FROM f.approbateurs u2)) " +
       "AND (LOWER(COALESCE(f.numeroFacture, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')) " +
       "     OR (f.client IS NOT NULL AND LOWER(COALESCE(f.client.nomComplet, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
       "     OR (f.entrepriseClient IS NOT NULL AND LOWER(COALESCE(f.entrepriseClient.nom, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')))) " +
       "ORDER BY f.dateCreation DESC, f.id DESC")
Page<FactureProForma> findFacturesAvecRelationsParEntrepriseEtUtilisateurPaginatedWithSearch(
    @Param("entrepriseId") Long entrepriseId,
    @Param("userId") Long userId,
    @Param("search") String search,
    Pageable pageable);

// Méthode de pagination pour utilisateur avec permission facturation (créateur, approbateur ou destinataire de note)
@Query("SELECT DISTINCT f FROM FactureProForma f " +
       "LEFT JOIN FETCH f.approbateurs a " +
       "LEFT JOIN FETCH f.utilisateurCreateur u " +
       "LEFT JOIN FETCH f.client c " +
       "LEFT JOIN FETCH f.entrepriseClient ec " +
       "LEFT JOIN FETCH f.entreprise e " +
       "WHERE f.entreprise.id = :entrepriseId " +
       "AND (f.utilisateurCreateur.id = :userId " +
       "     OR f.utilisateurApprobateur.id = :userId " +
       "     OR :userId IN (SELECT u2.id FROM f.approbateurs u2) " +
       "     OR f.id IN (SELECT n.facture.id FROM NoteFactureProForma n WHERE n.destinataire.id = :userId)) " +
       "ORDER BY f.dateCreation DESC, f.id DESC")
Page<FactureProForma> findFacturesAvecRelationsParEntrepriseEtUtilisateurAvecNotesPaginated(
    @Param("entrepriseId") Long entrepriseId,
    @Param("userId") Long userId,
    Pageable pageable);

@Query("SELECT DISTINCT f FROM FactureProForma f " +
       "LEFT JOIN FETCH f.approbateurs a " +
       "LEFT JOIN FETCH f.utilisateurCreateur u " +
       "LEFT JOIN FETCH f.client c " +
       "LEFT JOIN FETCH f.entrepriseClient ec " +
       "LEFT JOIN FETCH f.entreprise e " +
       "WHERE f.entreprise.id = :entrepriseId " +
       "AND (f.utilisateurCreateur.id = :userId " +
       "     OR f.utilisateurApprobateur.id = :userId " +
       "     OR :userId IN (SELECT u2.id FROM f.approbateurs u2) " +
       "     OR f.id IN (SELECT n.facture.id FROM NoteFactureProForma n WHERE n.destinataire.id = :userId)) " +
       "AND (LOWER(COALESCE(f.numeroFacture, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')) " +
       "     OR (f.client IS NOT NULL AND LOWER(COALESCE(f.client.nomComplet, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
       "     OR (f.entrepriseClient IS NOT NULL AND LOWER(COALESCE(f.entrepriseClient.nom, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')))) " +
       "ORDER BY f.dateCreation DESC, f.id DESC")
Page<FactureProForma> findFacturesAvecRelationsParEntrepriseEtUtilisateurAvecNotesPaginatedWithSearch(
    @Param("entrepriseId") Long entrepriseId,
    @Param("userId") Long userId,
    @Param("search") String search,
    Pageable pageable);

// Compter le nombre total de factures par entreprise
@Query("SELECT COUNT(f) FROM FactureProForma f WHERE f.entreprise.id = :entrepriseId")
long countFacturesByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

// Compter le nombre de factures par statut pour une entreprise
@Query("SELECT COUNT(f) FROM FactureProForma f " +
       "WHERE f.entreprise.id = :entrepriseId AND f.statut = :statut")
long countFacturesByEntrepriseIdAndStatut(@Param("entrepriseId") Long entrepriseId, @Param("statut") StatutFactureProForma statut);

// Compter le nombre de factures par utilisateur pour une entreprise
@Query("SELECT COUNT(f) FROM FactureProForma f " +
       "WHERE f.entreprise.id = :entrepriseId " +
       "AND (f.utilisateurCreateur.id = :userId " +
       "     OR f.utilisateurApprobateur.id = :userId " +
       "     OR :userId IN (SELECT u.id FROM f.approbateurs u))")
long countFacturesByEntrepriseIdAndUtilisateur(@Param("entrepriseId") Long entrepriseId, @Param("userId") Long userId);

// Récupère les factures proforma en attente (BROUILLON ou EN_ATTENTE_VALIDATION) triées par date
@Query("SELECT f FROM FactureProForma f WHERE f.entreprise.id = :entrepriseId " +
       "AND (f.statut = 'BROUILLON' OR f.statut = 'EN_ATTENTE_VALIDATION') " +
       "ORDER BY f.dateCreation DESC")
    List<FactureProForma> findFacturesProformaEnAttenteByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
}
