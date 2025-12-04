package com.xpertcash.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Role;
import com.xpertcash.entity.Enum.RoleType;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>{

    /**
     * Retourne un rôle par son RoleType.
     * Depuis que plusieurs rôles peuvent partager le même name (clonage de rôles),
     * on utilise une requête native avec LIMIT 1 pour éviter les erreurs
     * IncorrectResultSizeDataAccessException lorsqu'il existe plusieurs entrées.
     */
    @Query(value = "SELECT * FROM role r WHERE r.name = :name LIMIT 1", nativeQuery = true)
    Optional<Role> findByName(@Param("name") RoleType name);

    boolean existsByName(RoleType name);
}
