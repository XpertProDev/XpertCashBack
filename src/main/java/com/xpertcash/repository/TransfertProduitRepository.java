package com.xpertcash.repository;

import com.xpertcash.entity.TransfertProduit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransfertProduitRepository extends JpaRepository<TransfertProduit, Long> {
    List<TransfertProduit> findByMagasinId(Long magasinId);
    List<TransfertProduit> findByBoutiqueId(Long boutiqueId);
}

