package com.xpertcash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.FactureProForma;
import com.xpertcash.entity.LigneFactureProforma;

@Repository
public interface LigneFactureProformaRepository  extends JpaRepository<LigneFactureProforma, Long> {
    boolean existsByProduitId(Long produitId);

    void deleteByFactureProForma(FactureProForma facture);

    

}
