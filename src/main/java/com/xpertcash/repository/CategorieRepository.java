package com.xpertcash.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Categorie;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long>{
    Optional<Categorie> findByNom(String nom);
    boolean existsByNom(String nom);

}
