package com.xpertcash.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Unite;

@Repository
public interface UniteRepository extends JpaRepository<Unite, Long>{
    Optional<Unite> findByNom(String nom);
    
    boolean existsByNom(String nom);

    Optional<Unite> findById(Long id);
    


}
