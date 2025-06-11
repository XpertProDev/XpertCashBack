package com.xpertcash.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.FactureReelle;
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

     // 🔍 Trouver les factures à relancer qui n'ont jamais reçu de rappel
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



    
}


