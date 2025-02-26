package com.xpertcash.repository;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.ProduitBoutique;
import com.xpertcash.entity.Produits;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProduitBoutiqueRepository extends JpaRepository<ProduitBoutique, Long> {
    Optional<ProduitBoutique> findByProduitAndBoutique(Produits produit, Boutique boutique);
}

