package com.xpertcash.repository;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Enum.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {

    // Recherche par nom d'entreprise (optimisé avec JOIN FETCH pour admin et modules)
    @Query("SELECT e FROM Entreprise e " +
           "LEFT JOIN FETCH e.admin " +
           "LEFT JOIN FETCH e.modulesActifs " +
           "WHERE e.nomEntreprise = :nom")
    Optional<Entreprise> findByNomEntreprise(@Param("nom") String nom);

    // Vérifier l'existence par identifiant (optimisé avec JOIN pour comptage)
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Entreprise e " +
           "WHERE e.identifiantEntreprise = :identifiantEntreprise")
    boolean existsByIdentifiantEntreprise(@Param("identifiantEntreprise") String identifiantEntreprise);

    // Récupérer toutes les entreprises avec relations (optimisé avec JOIN FETCH pour admin et modules)
    @Query("SELECT DISTINCT e FROM Entreprise e " +
           "LEFT JOIN FETCH e.admin " +
           "LEFT JOIN FETCH e.modulesActifs")
    List<Entreprise> findAllWithRelations();

    // Récupérer toutes les entreprises en excluant une entreprise par son nom (pour SUPER_ADMIN)
    @Query("SELECT DISTINCT e FROM Entreprise e " +
           "LEFT JOIN FETCH e.admin " +
           "LEFT JOIN FETCH e.modulesActifs " +
           "WHERE e.nomEntreprise <> :excludedName")
    Page<Entreprise> findAllExcludingNomEntreprise(@Param("excludedName") String excludedName, Pageable pageable);

    /** Exclut les entreprises dont l'admin a le rôle SUPER_ADMIN (compte technique plateforme). */
    @Query(value = "SELECT e FROM Entreprise e " +
            "WHERE NOT EXISTS (SELECT e2.id FROM Entreprise e2 INNER JOIN e2.admin a2 INNER JOIN a2.role r2 WHERE e2.id = e.id AND r2.name = :excludedRole)",
            countQuery = "SELECT COUNT(e) FROM Entreprise e " +
            "WHERE NOT EXISTS (SELECT e2.id FROM Entreprise e2 INNER JOIN e2.admin a2 INNER JOIN a2.role r2 WHERE e2.id = e.id AND r2.name = :excludedRole)")
    Page<Entreprise> findAllExcludingAdminRole(@Param("excludedRole") RoleType excludedRole, Pageable pageable);

    /** Compte les entreprises dont l'admin n'est pas SUPER_ADMIN (pour dashboard). */
    @Query("SELECT COUNT(e) FROM Entreprise e " +
           "LEFT JOIN e.admin a " +
           "LEFT JOIN a.role r " +
           "WHERE (a IS NULL OR r.name <> :excludedRole)")
    long countExcludingAdminRole(@Param("excludedRole") RoleType excludedRole);

    /** Compte les entreprises actives (admin <> SUPER_ADMIN et active = true). */
    @Query("SELECT COUNT(e) FROM Entreprise e " +
           "LEFT JOIN e.admin a " +
           "LEFT JOIN a.role r " +
           "WHERE (a IS NULL OR r.name <> :excludedRole) AND (e.active = true OR e.active IS NULL)")
    long countExcludingAdminRoleAndActiveTrue(@Param("excludedRole") RoleType excludedRole);

    /** Compte les entreprises désactivées (admin <> SUPER_ADMIN et active = false). */
    @Query("SELECT COUNT(e) FROM Entreprise e " +
           "LEFT JOIN e.admin a " +
           "LEFT JOIN a.role r " +
           "WHERE (a IS NULL OR r.name <> :excludedRole) AND e.active = false")
    long countExcludingAdminRoleAndActiveFalse(@Param("excludedRole") RoleType excludedRole);

    /** Compte les entreprises créées depuis une date (admin <> SUPER_ADMIN). */
    @Query("SELECT COUNT(e) FROM Entreprise e " +
           "LEFT JOIN e.admin a " +
           "LEFT JOIN a.role r " +
           "WHERE (a IS NULL OR r.name <> :excludedRole) AND e.createdAt >= :since")
    long countExcludingAdminRoleAndCreatedAtAfter(@Param("excludedRole") RoleType excludedRole, @Param("since") LocalDateTime since);
}
