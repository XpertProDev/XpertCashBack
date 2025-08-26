package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;

import jakarta.persistence.LockModeType;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {
    List<Stock> findByBoutiqueId(Long boutiqueId);

    Stock findByProduit(Produit produit);

    Stock findByBoutiqueAndProduit(Boutique boutique, Produit produit);
    List<Stock> findByBoutiqueAndStockActuelGreaterThan(Boutique boutique, Integer quantite);



    // 🔒 Récupère tous les stocks liés à une liste de produits
    // en posant un verrou PESSIMISTIC_WRITE (bloque la ligne pendant la transaction)
    @Query("SELECT s FROM Stock s WHERE s.produit.id IN :ids")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Stock> findAllByProduitIdInWithLock(@Param("ids") List<Long> ids);


}

