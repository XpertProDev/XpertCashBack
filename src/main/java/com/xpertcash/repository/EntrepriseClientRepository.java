package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.EntrepriseClient;

@Repository
public interface EntrepriseClientRepository extends JpaRepository<EntrepriseClient, Long>{


    // Méthodes isolantes (filtrées par entreprise)
    // Recherche par email et entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT e FROM EntrepriseClient e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "WHERE e.email = :email AND ent.id = :entrepriseId")
    Optional<EntrepriseClient> findByEmailAndEntrepriseId(@Param("email") String email, @Param("entrepriseId") Long entrepriseId);

    // Recherche par téléphone et entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT e FROM EntrepriseClient e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "WHERE e.telephone = :telephone AND ent.id = :entrepriseId")
    Optional<EntrepriseClient> findByTelephoneAndEntrepriseId(@Param("telephone") String telephone, @Param("entrepriseId") Long entrepriseId);

    // Recherche par nom et entreprise (pour isolation)
    @Query("SELECT e FROM EntrepriseClient e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "WHERE e.nom = :nom AND ent.id = :entrepriseId")
    Optional<EntrepriseClient> findByNomAndEntrepriseId(@Param("nom") String nom, @Param("entrepriseId") Long entrepriseId);

    // Recherche par ID et entreprise (pour isolation)
    @Query("SELECT e FROM EntrepriseClient e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "WHERE e.id = :id AND ent.id = :entrepriseId")
    Optional<EntrepriseClient> findByIdAndEntrepriseId(@Param("id") Long id, @Param("entrepriseId") Long entrepriseId);

    // Récupérer toutes les entreprises clientes d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT e FROM EntrepriseClient e " +
           "LEFT JOIN FETCH e.entreprise ent " +
           "WHERE ent.id = :entrepriseId")
    List<EntrepriseClient> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    /** Pagination côté base : entreprises clientes de l'entreprise (tenant). */
    @Query(value = "SELECT e FROM EntrepriseClient e " +
           "LEFT JOIN e.entreprise ent " +
           "WHERE ent.id = :entrepriseId",
           countQuery = "SELECT COUNT(e) FROM EntrepriseClient e " +
           "LEFT JOIN e.entreprise ent " +
           "WHERE ent.id = :entrepriseId")
    Page<EntrepriseClient> findByEntrepriseIdPaginated(
            @Param("entrepriseId") Long entrepriseId, Pageable pageable);

}
