package com.xpertcash.repository;

import com.xpertcash.entity.User;
import com.xpertcash.entity.Role;
import com.xpertcash.entity.Enum.RoleType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsersRepository extends JpaRepository<User, Long> {
    
    // Méthodes de base (gardées pour compatibilité avec register/login)
    Optional<User> findByUuid(String uuid);
    Optional<User> findByEmail(String email); // Gardé pour register/login
    Optional<User> findByPhone(String phone); // Gardé pour register/login
    
    // Méthodes isolées par entreprise (optimisées avec JOIN FETCH)
    
    // Recherche par email et entreprise (pour isolation)
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.entreprise e " +
           "LEFT JOIN FETCH u.role r " +
           "WHERE u.email = :email AND e.id = :entrepriseId")
    Optional<User> findByEmailAndEntrepriseId(
            @Param("email") String email,
            @Param("entrepriseId") Long entrepriseId);
    
    // Recherche par téléphone et entreprise (pour isolation)
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.entreprise e " +
           "LEFT JOIN FETCH u.role r " +
           "WHERE u.phone = :phone AND e.id = :entrepriseId")
    Optional<User> findByPhoneAndEntrepriseId(
            @Param("phone") String phone,
            @Param("entrepriseId") Long entrepriseId);
    
    // Recherche par téléphone, pays et entreprise (pour isolation)
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.entreprise e " +
           "LEFT JOIN FETCH u.role r " +
           "WHERE u.phone = :phone AND u.pays = :pays AND e.id = :entrepriseId")
    Optional<User> findByPhoneAndPaysAndEntrepriseId(
            @Param("phone") String phone,
            @Param("pays") String pays,
            @Param("entrepriseId") Long entrepriseId);

    // Récupérer tous les utilisateurs d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.entreprise e " +
           "LEFT JOIN FETCH u.role r " +
           "WHERE e.id = :entrepriseId")
    List<User> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Compter tous les utilisateurs d'une entreprise (optimisé)
    @Query("SELECT COUNT(DISTINCT u) FROM User u " +
           "INNER JOIN u.entreprise e " +
           "WHERE e.id = :entrepriseId")
    long countByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    /** Dernière activité (connexion) parmi tous les utilisateurs de l'entreprise. */
    @Query("SELECT MAX(u.lastActivity) FROM User u WHERE u.entreprise.id = :entrepriseId")
    Optional<LocalDateTime> findMaxLastActivityByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    /** Batch : count par entreprise pour une liste d'IDs (évite N requêtes). Retourne [entrepriseId, count]. */
    @Query("SELECT u.entreprise.id, COUNT(u) FROM User u " +
           "WHERE u.entreprise.id IN :ids AND u.role.name <> :excludedRole " +
           "GROUP BY u.entreprise.id")
    List<Object[]> countByEntrepriseIdIn(@Param("ids") List<Long> ids,
                                         @Param("excludedRole") RoleType excludedRole);

    /** Batch : max lastActivity par entreprise pour une liste d'IDs. Retourne [entrepriseId, maxLastActivity]. */
    @Query("SELECT u.entreprise.id, MAX(u.lastActivity) FROM User u WHERE u.entreprise.id IN :ids GROUP BY u.entreprise.id")
    List<Object[]> findMaxLastActivityByEntrepriseIdIn(@Param("ids") List<Long> ids);

    /** Total utilisateurs (hors SUPER_ADMIN) dans les entreprises dont l'admin n'est pas SUPER_ADMIN (dashboard). */
    @Query("SELECT COUNT(u) FROM User u " +
           "JOIN u.entreprise e " +
           "LEFT JOIN e.admin a " +
           "LEFT JOIN a.role r " +
           "WHERE u.role.name <> :excludedRole AND (a IS NULL OR r.name <> :excludedRole)")
    long countUsersInNonSuperAdminEntreprises(@Param("excludedRole") RoleType excludedRole);

    // Récupérer tous les utilisateurs d'une entreprise sauf l'ADMIN (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.entreprise e " +
           "LEFT JOIN FETCH u.role r " +
           "WHERE e.id = :entrepriseId AND u.id <> :adminId")
    List<User> findByEntrepriseIdAndIdNot(
            @Param("entrepriseId") Long entrepriseId,
            @Param("adminId") Long adminId);

    // Vérifier l'existence d'un code personnel (global - code PIN unique dans toute la base)
    boolean existsByPersonalCode(String personalCode);
    
    // Utilisateurs partageant un même rôle (global - utilisé pour réutilisation de rôles)
    List<User> findByRole(Role role);
    
    // Utilisateurs d'un rôle spécifique ET d'une entreprise spécifique (pour isolation)
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.entreprise e " +
           "LEFT JOIN FETCH u.role r " +
           "WHERE u.role = :role AND e.id = :entrepriseId")
    List<User> findByRoleAndEntrepriseId(
            @Param("role") Role role,
            @Param("entrepriseId") Long entrepriseId);

    // Recherche par entreprise et rôles (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.entreprise e " +
           "LEFT JOIN FETCH u.role r " +
           "WHERE e.id = :entrepriseId AND r.name IN :roles")
    Optional<User> findByEntrepriseIdAndRole_NameIn(
            @Param("entrepriseId") Long entrepriseId,
            @Param("roles") List<RoleType> roles);

    // Recherche par boutique et rôle (pour isolation)
    @Query("SELECT DISTINCT u FROM User u " +
           "INNER JOIN u.userBoutiques ub " +
           "INNER JOIN ub.boutique b " +
           "LEFT JOIN FETCH u.entreprise e " +
           "LEFT JOIN FETCH u.role r " +
           "WHERE b.id = :boutiqueId " +
           "AND r.name = :roleName " +
           "AND e.id = :entrepriseId")
    List<User> findByBoutiqueIdAndRole_NameAndEntrepriseId(
            @Param("boutiqueId") Long boutiqueId,
            @Param("roleName") RoleType roleName,
            @Param("entrepriseId") Long entrepriseId);

    // Compter les utilisateurs d'une entreprise excluant un rôle (optimisé)
    @Query("SELECT COUNT(DISTINCT u) FROM User u " +
           "INNER JOIN u.entreprise e " +
           "WHERE e.id = :entrepriseId AND u.role.name <> :excludedRole")
    long countByEntrepriseIdExcludingRole(
            @Param("entrepriseId") Long entrepriseId,
            @Param("excludedRole") RoleType excludedRole);

    // Récupérer l'utilisateur avec entreprise et role en une seule requête
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN FETCH u.entreprise e " +
           "JOIN FETCH u.role r " +
           "LEFT JOIN FETCH r.permissions p " +
           "WHERE u.id = :userId")
    Optional<User> findByIdWithEntrepriseAndRole(@Param("userId") Long userId);

    // Récupérer l'utilisateur par UUID avec entreprise et role en une seule requête
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN FETCH u.entreprise e " +
           "JOIN FETCH u.role r " +
           "LEFT JOIN FETCH r.permissions p " +
           "WHERE u.uuid = :uuid")
    Optional<User> findByUuidWithEntrepriseAndRole(@Param("uuid") String uuid);

    // Récupérer l'utilisateur par ID et entreprise (pour isolation)
    @Query("SELECT DISTINCT u FROM User u " +
           "LEFT JOIN FETCH u.entreprise e " +
           "LEFT JOIN FETCH u.role r " +
           "LEFT JOIN FETCH r.permissions p " +
           "WHERE u.id = :userId AND e.id = :entrepriseId")
    Optional<User> findByIdAndEntrepriseId(
            @Param("userId") Long userId,
            @Param("entrepriseId") Long entrepriseId);

    // Mettre à jour uniquement lastActivity sans verrouiller toute la ligne (évite les deadlocks)
    @Modifying
    @Query("UPDATE User u SET u.lastActivity = :lastActivity WHERE u.id = :userId")
    void updateLastActivity(@Param("userId") Long userId, @Param("lastActivity") LocalDateTime lastActivity);

    // Récupérer les utilisateurs ayant une permission spécifique dans une entreprise
    @Query("SELECT DISTINCT u FROM User u " +
           "JOIN FETCH u.entreprise e " +
           "JOIN FETCH u.role r " +
           "JOIN r.permissions p " +
           "WHERE e.id = :entrepriseId AND p.type = :permissionType")
    List<User> findByEntrepriseIdAndPermission(
            @Param("entrepriseId") Long entrepriseId,
            @Param("permissionType") com.xpertcash.entity.PermissionType permissionType);

}

