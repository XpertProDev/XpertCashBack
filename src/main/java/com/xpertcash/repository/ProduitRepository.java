package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Produit;
import com.xpertcash.entity.Stock;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    Optional<Produit> findByNom(String nom);
    boolean existsByCodeGenerique(String codeGenerique);
    @Query("SELECT p FROM Produit p WHERE p.nom = :nom AND p.prixVente = :prixVente AND p.boutique.id = :boutiqueId")
    Produit findByNomAndPrixVenteAndBoutiqueId(String nom, Double prixVente, Long boutiqueId);

    @Query("SELECT p FROM Produit p WHERE p.nom = :nom  AND p.boutique.id = :boutiqueId")
    Produit findByNomAndBoutiqueId(String nom, Long boutiqueId);

    @Query("SELECT p FROM Produit p WHERE p.codeBare = :codeBare AND p.boutique.id = :boutiqueId")
    Produit findByCodeBareAndBoutiqueId(String codeBare, Long boutiqueId);


    List<Produit> findByBoutique(Boutique boutique);

     // Récupérer les produits enStock = false d'une boutique donnée
     List<Produit> findByBoutiqueIdAndEnStockFalse(Long boutiqueId);

     // Récupérer les produits enStock = true d'une boutique donnée
     List<Produit> findByBoutiqueIdAndEnStockTrue(Long boutiqueId);

   


}

