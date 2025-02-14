package com.xpertcash.repository;

import com.xpertcash.entity.CategoryProduit;
import com.xpertcash.entity.Produits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitsRepository extends JpaRepository<Produits, Long> {

    Optional<Produits> findById(Long id);
    public Optional<Produits> findByNomProduitAndCategory(String nomProduit, CategoryProduit category);

    // Méthode pour trouver tous les produits d'une catégorie donnée
    List<Produits> findByCategory(CategoryProduit category);

    // Autres méthodes, par exemple pour trouver un produit par son nom et catégorie
    //Optional<Produits> findByNomProduitAndCategory(String nomProduit, CategoryProduit category);

}