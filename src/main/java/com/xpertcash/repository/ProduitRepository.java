package com.xpertcash.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Produit;

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

    List<Produit> findByBoutiqueIdAndEnStockFalse(Long boutiqueId);

    List<Produit> findByBoutiqueIdAndEnStockTrue(Long boutiqueId);

    @Query("SELECT p FROM Produit p WHERE p.boutique.id = :boutiqueId AND p.id = :produitId")
    Optional<Produit> findByBoutiqueAndId(@Param("boutiqueId") Long boutiqueId, @Param("produitId") Long produitId);

    @Query("SELECT p FROM Produit p WHERE p.boutique.id = :boutiqueId AND p.codeGenerique = :codeGenerique")
    Optional<Produit> findByBoutiqueAndCodeGenerique(@Param("boutiqueId") Long boutiqueId, @Param("codeGenerique") String codeGenerique);

    @Query("SELECT p FROM Produit p WHERE p.boutique.entreprise.id = :entrepriseId")
    List<Produit> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
    
    @Query("SELECT p FROM Produit p WHERE p.nom = :nom AND p.boutique.entreprise.id = :entrepriseId")
    Produit findByNomAndEntrepriseId(@Param("nom") String nom, @Param("entrepriseId") Long entrepriseId);

    List<Produit> findByCodeGenerique(String codeGenerique);



}

