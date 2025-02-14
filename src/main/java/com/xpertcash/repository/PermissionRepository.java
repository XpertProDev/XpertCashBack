package com.xpertcash.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xpertcash.entity.Permission;
import com.xpertcash.entity.PermissionType;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    // Trouver une permission par son type
    Optional<Permission> findByType(PermissionType type);

    // Vérifier si une permission existe déjà par son type
    boolean existsByType(PermissionType type);

}
