package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Categorie;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, Long>{
    Optional<Categorie> findByNom(String nom);
    boolean existsByNom(String nom);
    boolean existsByNomAndEntrepriseId(String nom, Long entrepriseId);

    Categorie findByNomAndEntrepriseId(String nom, Long entrepriseId);

    // Méthode de pagination par entreprise
    Page<Categorie> findByEntrepriseId(Long entrepriseId, Pageable pageable);
    
    // Méthode pour récupérer une catégorie par ID et entreprise
    Optional<Categorie> findByIdAndEntrepriseId(Long id, Long entrepriseId);
    
    // Méthode pour récupérer toutes les catégories d'une entreprise (sans pagination)
    List<Categorie> findByEntrepriseId(Long entrepriseId);
}
