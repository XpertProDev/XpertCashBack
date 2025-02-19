package com.xpertcash.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    // Trouver le stock d'un produit spécifique
    Optional<Stock> findByProduitId(Long produitId);

    // Supprimer le stock d'un produit (si besoin)
    void deleteByProduitId(Long produitId);


}
