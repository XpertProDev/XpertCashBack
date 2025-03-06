package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByBoutiqueId(Long boutiqueId);

    Stock findByProduit(Produit produit);

    Stock findByBoutiqueAndProduit(Boutique boutique, Produit produit);
    List<Stock> findByBoutiqueAndStockActuelGreaterThan(Boutique boutique, Integer quantite);


}

