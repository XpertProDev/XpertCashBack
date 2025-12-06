package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Role;
import com.xpertcash.entity.Enum.RoleType;

    @Repository
    public interface RoleRepository extends JpaRepository<Role, Long>{
        Optional<Role> findByName(RoleType name );
        boolean existsByName(RoleType name);
        
        // Méthode pour récupérer le premier rôle d'un type donné (gère les cas où plusieurs rôles existent)
        @Query("SELECT r FROM Role r WHERE r.name = :name ORDER BY r.id ASC")
        List<Role> findAllByName(@Param("name") RoleType name);
        
        // Méthode pour récupérer le premier rôle d'un type donné
        default Optional<Role> findFirstByName(RoleType name) {
            List<Role> roles = findAllByName(name);
            return roles.isEmpty() ? Optional.empty() : Optional.of(roles.get(0));
        }
    }
