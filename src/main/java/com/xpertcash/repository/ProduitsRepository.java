package com.xpertcash.repository;

import com.xpertcash.entity.CategoryProduit;
import com.xpertcash.entity.Produits;
import com.xpertcash.entity.UniteMesure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitsRepository extends JpaRepository<Produits, Long> {

    Optional<Produits> findById(Long id);
    public Optional<Produits> findByNomProduitAndCategory(String nomProduit, CategoryProduit category);

    // Méthode pour trouver tous les produits d'une catégorie donnée
    List<Produits> findByCategory(CategoryProduit category);

    List<Produits> findByCategoryIn(List<CategoryProduit> categories);

    Optional<Produits> findByNomProduitAndCategoryAndUniteMesure(String nomProduit, CategoryProduit category, UniteMesure uniteMesure);

    // Méthode pour rechercher un produit par codebar
    Optional<Produits> findByCodebar(String codebar);

    @Query("SELECT p.magasin.id, COUNT(p), SUM(p.quantite) FROM Produits p GROUP BY p.magasin.id")
    List<Object[]> countAndSumQuantitiesByMagasin();




}





