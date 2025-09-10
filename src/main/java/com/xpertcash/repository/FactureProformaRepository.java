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

    @Query("SELECT f FROM FactureProForma f " +
           "WHERE f.statut = :statut " +
           "AND (:clientId IS NULL OR f.client.id = :clientId) " +
           "AND (:entrepriseClientId IS NULL OR f.entrepriseClient.id = :entrepriseClientId)")
    List<FactureProForma> findExistingFactures(@Param("clientId") Long clientId, 
                                               @Param("entrepriseClientId") Long entrepriseClientId,
                                               @Param("statut") StatutFactureProForma statut);

    Optional<FactureProForma> findTopByDateCreationOrderByNumeroFactureDesc(LocalDate dateCreation);

     // Trouver les factures à relancer qui n'ont jamais reçu de rappel
    // ou dont le dernier rappel a été envoyé avant aujourd'hui
    @Query("SELECT f FROM FactureProForma f WHERE f.dateRelance < :now AND (f.dernierRappelEnvoye IS NULL OR f.dernierRappelEnvoye < :now)")
    List<FactureProForma> findByDateRelanceBeforeAndDernierRappelEnvoyeIsNullOrDernierRappelEnvoyeBefore(@Param("now") LocalDateTime now);

    @Query("SELECT f FROM FactureProForma f WHERE f.dateRelance <= :now AND f.dernierRappelEnvoye IS NULL AND f.notifie = false")
    List<FactureProForma> findFacturesAEnvoyer(@Param("now") LocalDateTime now);

    @Query("SELECT f FROM FactureProForma f WHERE FUNCTION('YEAR', f.dateCreation) = :year ORDER BY f.numeroFacture DESC")
    List<FactureProForma> findFacturesDeLAnnee(@Param("year") int year);

    @Query("SELECT f FROM FactureProForma f WHERE f.entreprise.id = :entrepriseId ORDER BY f.dateCreation DESC, f.id DESC")
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


    @Query("SELECT f FROM FactureProForma f WHERE " +
       "(:clientId IS NOT NULL AND f.client.id = :clientId) OR " +
       "(:entrepriseClientId IS NOT NULL AND f.entrepriseClient.id = :entrepriseClientId)")
    List<FactureProForma> findByClientIdOrEntrepriseClientId(@Param("clientId") Long clientId,
                                                         @Param("entrepriseClientId") Long entrepriseClientId);

    boolean existsByClientId(Long clientId);
    boolean existsByEntrepriseClientId(Long entrepriseClientId);

    List<FactureProForma> findByEntrepriseIdAndDateCreationBetween(Long entrepriseId, LocalDateTime start, LocalDateTime end);


    List<FactureProForma> findByUtilisateurCreateur_Id(Long userId);


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

// Méthode de pagination avec filtrage par utilisateur
@Query("SELECT f FROM FactureProForma f " +
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
    
}


