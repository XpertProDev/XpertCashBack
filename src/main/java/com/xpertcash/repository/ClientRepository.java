package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Client;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long>{
    List<Client> findByEntrepriseClientId(Long entrepriseClientId);

    // Recherche par email avec filtre par entreprise (optimisé avec JOIN)
    @Query("SELECT c FROM Client c " +
           "LEFT JOIN c.entreprise e " +
           "LEFT JOIN c.entrepriseClient ec " +
           "LEFT JOIN ec.entreprise ece " +
           "WHERE c.email = :email AND " +
           "(e.id = :entrepriseId OR (ec IS NOT NULL AND ece.id = :entrepriseId))")
    Optional<Client> findByEmailAndEntrepriseId(@Param("email") String email, @Param("entrepriseId") Long entrepriseId);

    // Recherche par téléphone avec filtre par entreprise (optimisé avec JOIN)
    @Query("SELECT c FROM Client c " +
           "LEFT JOIN c.entreprise e " +
           "LEFT JOIN c.entrepriseClient ec " +
           "LEFT JOIN ec.entreprise ece " +
           "WHERE c.telephone = :telephone AND " +
           "(e.id = :entrepriseId OR (ec IS NOT NULL AND ece.id = :entrepriseId))")
    Optional<Client> findByTelephoneAndEntrepriseId(@Param("telephone") String telephone, @Param("entrepriseId") Long entrepriseId);

    // Requête optimisée pour charger les relations et filtrer par entreprise
    @Query("SELECT DISTINCT c FROM Client c " +
           "LEFT JOIN FETCH c.entreprise e " +
           "LEFT JOIN FETCH c.entrepriseClient ec " +
           "LEFT JOIN FETCH ec.entreprise ece " +
           "WHERE (e.id = :entrepriseId) " +
           "OR (ec IS NOT NULL AND ece.id = :entrepriseId)")
    List<Client> findClientsByEntrepriseOrEntrepriseClient(@Param("entrepriseId") Long entrepriseId);

    /** Pagination côté base : clients de l'entreprise (tenant). */
    @Query(value = "SELECT c FROM Client c " +
           "LEFT JOIN c.entreprise e " +
           "LEFT JOIN c.entrepriseClient ec " +
           "LEFT JOIN ec.entreprise ece " +
           "WHERE (e.id = :entrepriseId) OR (ec IS NOT NULL AND ece.id = :entrepriseId)",
           countQuery = "SELECT COUNT(c) FROM Client c " +
           "LEFT JOIN c.entreprise e " +
           "LEFT JOIN c.entrepriseClient ec " +
           "LEFT JOIN ec.entreprise ece " +
           "WHERE (e.id = :entrepriseId) OR (ec IS NOT NULL AND ece.id = :entrepriseId)")
    Page<Client> findClientsByEntrepriseOrEntrepriseClientPaginated(
            @Param("entrepriseId") Long entrepriseId, Pageable pageable);

    /** Pagination + recherche côté base : nom, email ou téléphone contient le terme (insensible à la casse). */
    @Query(value = "SELECT c FROM Client c " +
           "LEFT JOIN c.entreprise e " +
           "LEFT JOIN c.entrepriseClient ec " +
           "LEFT JOIN ec.entreprise ece " +
           "WHERE ((e.id = :entrepriseId) OR (ec IS NOT NULL AND ece.id = :entrepriseId)) " +
           "AND (LOWER(COALESCE(c.nomComplet, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR COALESCE(c.telephone, '') LIKE CONCAT('%', :search, '%'))",
           countQuery = "SELECT COUNT(c) FROM Client c " +
           "LEFT JOIN c.entreprise e " +
           "LEFT JOIN c.entrepriseClient ec " +
           "LEFT JOIN ec.entreprise ece " +
           "WHERE ((e.id = :entrepriseId) OR (ec IS NOT NULL AND ece.id = :entrepriseId)) " +
           "AND (LOWER(COALESCE(c.nomComplet, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(COALESCE(c.email, '')) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR COALESCE(c.telephone, '') LIKE CONCAT('%', :search, '%'))")
    Page<Client> findClientsByEntrepriseOrEntrepriseClientPaginatedWithSearch(
            @Param("entrepriseId") Long entrepriseId, @Param("search") String search, Pageable pageable);

    // Compter séparément les clients rattachés directement à l'entreprise (PARTICULIERS)
    @Query("SELECT COUNT(c) FROM Client c " +
           "INNER JOIN c.entreprise e " +
           "WHERE e.id = :entrepriseId")
    long countClientsDirectByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Compter les clients rattachés via une EntrepriseClient (ENTREPRISE_CLIENT)
    @Query("SELECT COUNT(c) FROM Client c " +
           "INNER JOIN c.entrepriseClient ec " +
           "INNER JOIN ec.entreprise ece " +
           "WHERE ece.id = :entrepriseId")
    long countClientsEntrepriseByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    

}
