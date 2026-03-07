package com.xpertcash.repository;

import com.xpertcash.entity.EntreeGenerale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntreeGeneraleRepository extends JpaRepository<EntreeGenerale, Long> {
    
    // Récupérer toutes les entrées générales d'une entreprise triées par date (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT e FROM EntreeGenerale e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "LEFT JOIN FETCH e.categorie " +
           "WHERE ent.id = :entrepriseId " +
           "ORDER BY e.dateCreation DESC")
    List<EntreeGenerale> findByEntrepriseIdOrderByDateCreationDesc(@Param("entrepriseId") Long entrepriseId);
    
    // Récupérer toutes les entrées générales d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT e FROM EntreeGenerale e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "LEFT JOIN FETCH e.categorie " +
           "WHERE ent.id = :entrepriseId")
    List<EntreeGenerale> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
    
    // Récupérer les entrées générales par entreprise, mois et année (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT e FROM EntreeGenerale e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "LEFT JOIN FETCH e.categorie " +
           "WHERE ent.id = :entrepriseId " +
           "AND MONTH(e.dateCreation) = :month AND YEAR(e.dateCreation) = :year " +
           "ORDER BY e.numero DESC")
    List<EntreeGenerale> findByEntrepriseIdAndMonthAndYear(
            @Param("entrepriseId") Long entrepriseId,
            @Param("month") int month,
            @Param("year") int year);
    
    /**
     * Trouve une EntreeGenerale par son ID et l'ID de l'entreprise (sécurité)
     * Retourne Optional.empty() si l'entrée n'existe pas ou n'appartient pas à l'entreprise
     * Optimisé avec JOIN FETCH pour charger les relations
     */
    @Query("SELECT e FROM EntreeGenerale e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "LEFT JOIN FETCH e.categorie " +
           "WHERE e.id = :id AND ent.id = :entrepriseId")
    Optional<EntreeGenerale> findByIdAndEntrepriseId(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);
    
    // Récupérer les entrées générales d'une entreprise dans une période (optimisé)
    @Query("SELECT DISTINCT e FROM EntreeGenerale e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "LEFT JOIN FETCH e.categorie " +
           "WHERE ent.id = :entrepriseId " +
           "AND e.dateCreation >= :dateDebut AND e.dateCreation < :dateFin " +
           "ORDER BY e.dateCreation DESC")
    List<EntreeGenerale> findByEntrepriseIdAndDateCreationBetween(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    /** Pour pagination scalable : charger par IDs avec relations (évite N+1). */
    @Query("SELECT DISTINCT e FROM EntreeGenerale e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "LEFT JOIN FETCH e.categorie " +
           "LEFT JOIN FETCH e.creePar " +
           "LEFT JOIN FETCH e.responsable " +
           "WHERE e.id IN :ids")
    List<EntreeGenerale> findByIdInWithDetails(@Param("ids") List<Long> ids);

    /** Batch : max dateCreation par entreprise (dernière utilisation métier). Retourne [entrepriseId, maxDate]. */
    @Query("SELECT e.entreprise.id, MAX(e.dateCreation) FROM EntreeGenerale e WHERE e.entreprise.id IN :ids GROUP BY e.entreprise.id")
    List<Object[]> findMaxDateCreationByEntrepriseIdIn(@Param("ids") List<Long> ids);

    /** Trouve les entrées de type dette (ex. ECART_CAISSE) dont detteId est dans la liste (ex. IDs de caisses). */
    @Query("SELECT e FROM EntreeGenerale e WHERE e.detteType = :detteType AND e.detteId IN :detteIds")
    List<EntreeGenerale> findByDetteTypeAndDetteIdIn(
            @Param("detteType") String detteType,
            @Param("detteIds") List<Long> detteIds);

    /** Somme du montant restant dû (écart caisse) pour un responsable et une entreprise. */
    @Query(value = "SELECT COALESCE(SUM(e.montant_reste), 0) FROM entree_generale e " +
           "WHERE e.entreprise_id = :entrepriseId AND e.responsable_id = :responsableId " +
           "AND e.dette_type = 'ECART_CAISSE' AND e.source = 'DETTE'", nativeQuery = true)
    Double sumMontantResteEcartCaisseByResponsableAndEntreprise(
            @Param("responsableId") Long responsableId,
            @Param("entrepriseId") Long entrepriseId);

    /** Liste des dettes écart caisse (non soldées) pour un responsable et une entreprise, par date décroissante. */
    @Query("SELECT e FROM EntreeGenerale e WHERE e.entreprise.id = :entrepriseId AND e.responsable.id = :responsableId " +
           "AND e.detteType = 'ECART_CAISSE' AND e.source = :source ORDER BY e.dateCreation DESC")
    List<EntreeGenerale> findEcartCaisseByResponsableAndEntreprise(
            @Param("responsableId") Long responsableId,
            @Param("entrepriseId") Long entrepriseId,
            @Param("source") com.xpertcash.entity.Enum.SourceDepense source);
}

