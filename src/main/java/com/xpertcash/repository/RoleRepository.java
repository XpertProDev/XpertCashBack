package com.xpertcash.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xpertcash.entity.Role;
import com.xpertcash.entity.RoleType;

public interface RoleRepository extends JpaRepository<Role, Long>{
    Optional<Role> findByName(RoleType name );
    boolean existsByName(RoleType name);


    

}
