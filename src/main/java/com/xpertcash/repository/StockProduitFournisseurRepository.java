package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xpertcash.entity.StockProduitFournisseur;

public interface StockProduitFournisseurRepository extends JpaRepository<StockProduitFournisseur, Long> {
    List<StockProduitFournisseur> findByProduitId(Long produitId);

    @Query("SELECT spf.fournisseur.nomComplet, SUM(spf.quantiteAjoutee) FROM StockProduitFournisseur spf WHERE spf.produit.id = :produitId GROUP BY spf.fournisseur.nomComplet")
    List<Object[]> findQuantiteParFournisseurPourProduit(@Param("produitId") Long produitId);

    //Get quantite d'un produit par fournisseur
    @Query("SELECT spf.quantiteAjoutee FROM StockProduitFournisseur spf WHERE spf.produit.id = :produitId AND spf.fournisseur.id = :fournisseurId")
    Integer findQuantiteParFournisseur(@Param("produitId") Long produitId, @Param("fournisseurId") Long fournisseurId);

    //Get quantite d'un produit par fournisseur
    @Query("SELECT spf.produit.nom, spf.quantiteAjoutee FROM StockProduitFournisseur spf WHERE spf.fournisseur.id = :fournisseurId")
    List<Object[]> findNomProduitEtQuantiteAjoutee(@Param("fournisseurId") Long fournisseurId);

    


}
