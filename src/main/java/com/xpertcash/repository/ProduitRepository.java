package com.xpertcash.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Produit;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {
    Optional<Produit> findByNom(String nom);
    boolean existsByCodeGenerique(String codeGenerique);
    @Query("SELECT p FROM Produit p WHERE p.nom = :nom AND p.prixVente = :prixVente AND p.boutique.id = :boutiqueId")
    Produit findByNomAndPrixVenteAndBoutiqueId(String nom, Double prixVente, Long boutiqueId);

}

