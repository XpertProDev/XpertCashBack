package com.xpertcash.repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT p FROM Produit p WHERE p.boutique.id = :boutiqueId AND p.id IN :produitIds")
    List<Produit> findByBoutiqueAndIdIn(@Param("boutiqueId") Long boutiqueId, @Param("produitIds") List<Long> produitIds);

    List<Produit> findByDeletedTrueAndDeletedAtBefore(LocalDateTime date);

    // Pour les produits non supprimés et enStock = false
    List<Produit> findByBoutiqueIdAndEnStockFalseAndDeletedFalseOrDeletedIsNull(Long boutiqueId);
    
    // Pour les produits non supprimés et enStock = true
    List<Produit> findByBoutiqueIdAndEnStockTrueAndDeletedFalseOrDeletedIsNull(Long boutiqueId);

    List<Produit> findByBoutiqueIdAndDeletedTrue(Long boutiqueId);

    List<Produit> findByBoutiqueIdAndDeletedFalse(Long boutiqueId);


    
    @Query("SELECT p FROM Produit p WHERE p.boutique.id = :boutiqueId AND (p.deleted IS NULL OR p.deleted = false)")
    List<Produit> findByBoutiqueIdAndNotDeleted(@Param("boutiqueId") Long boutiqueId);

    List<Produit> findByBoutiqueIdAndDeletedFalseOrDeletedIsNull(Long boutiqueId);

    @Query("SELECT COUNT(p) FROM Produit p WHERE p.categorie.id = :categorieId AND p.boutique.entreprise.id = :entrepriseId")
long countByCategorieIdAndEntrepriseId(@Param("categorieId") Long categorieId, @Param("entrepriseId") Long entrepriseId);

@Query("SELECT p FROM Produit p WHERE p.boutique.entreprise.id = :entrepriseId")
List<Produit> findAllByEntrepriseId(@Param("entrepriseId") Long entrepriseId);



// Récupérer tous les produits d'une entreprise avec leurs catégories et boutiques
@Query("SELECT p FROM Produit p " +
       "JOIN FETCH p.categorie c " +
       "LEFT JOIN FETCH p.boutique b " +
       "WHERE b.entreprise.id = :entrepriseId OR p.boutique IS NULL")
List<Produit> findAllWithCategorieAndBoutiqueByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

// Compter les produits par catégorie pour une entreprise
@Query("SELECT p.categorie.id, COUNT(p) FROM Produit p " +
       "WHERE p.boutique.entreprise.id = :entrepriseId " +
       "GROUP BY p.categorie.id")
List<Object[]> countProduitsParCategorie(@Param("entrepriseId") Long entrepriseId);

// Compter les produits par catégorie pour une liste spécifique de catégories
@Query("SELECT p.categorie.id, COUNT(p) FROM Produit p " +
       "WHERE p.boutique.entreprise.id = :entrepriseId " +
       "AND p.categorie.id IN :categorieIds " +
       "GROUP BY p.categorie.id")
List<Object[]> countProduitsParCategorieIds(@Param("entrepriseId") Long entrepriseId, @Param("categorieIds") List<Long> categorieIds);

// Récupérer les produits par une liste de catégories pour une entreprise
@Query("SELECT p FROM Produit p " +
       "JOIN FETCH p.categorie c " +
       "LEFT JOIN FETCH p.boutique b " +
       "WHERE p.categorie.id IN :categorieIds " +
       "AND (b.entreprise.id = :entrepriseId OR b IS NULL)")
List<Produit> findByCategorieIdsAndEntrepriseId(@Param("categorieIds") List<Long> categorieIds, @Param("entrepriseId") Long entrepriseId);

// Récupérer les produits d'une catégorie spécifique avec pagination
@Query("SELECT p FROM Produit p " +
       "JOIN FETCH p.categorie c " +
       "LEFT JOIN FETCH p.boutique b " +
       "WHERE p.categorie.id = :categorieId " +
       "AND (b.entreprise.id = :entrepriseId OR b IS NULL)")
Page<Produit> findByCategorieIdAndEntrepriseIdPaginated(
    @Param("categorieId") Long categorieId, 
    @Param("entrepriseId") Long entrepriseId, 
    Pageable pageable);


// Récupérer tous les produits d'une entreprise avec la boutique jointe
@Query("SELECT p FROM Produit p LEFT JOIN FETCH p.boutique b " +
       "WHERE b.entreprise.id = :entrepriseId OR b IS NULL")
List<Produit> findAllWithBoutiqueByEntrepriseId(@Param("entrepriseId") Long entrepriseId);


// Récupérer tous les produits actifs d'une boutique avec les relations nécessaires
@Query("SELECT p FROM Produit p " +
       "LEFT JOIN FETCH p.boutique b " +
       "WHERE p.boutique.id = :boutiqueId AND (p.deleted IS NULL OR p.deleted = false)")
List<Produit> findActiveByBoutiqueIdWithRelations(@Param("boutiqueId") Long boutiqueId);

// Récupérer les produits d'une entreprise avec pagination et groupement par code générique
@Query("SELECT p FROM Produit p " +
       "LEFT JOIN FETCH p.boutique b " +
       "WHERE (b.entreprise.id = :entrepriseId OR b IS NULL) " +
       "AND (p.deleted IS NULL OR p.deleted = false) " +
       "ORDER BY p.codeGenerique, p.nom")
Page<Produit> findProduitsByEntrepriseIdPaginated(
    @Param("entrepriseId") Long entrepriseId, 
    Pageable pageable);

// Compter le nombre total de produits uniques par entreprise
@Query("SELECT COUNT(DISTINCT p.codeGenerique) FROM Produit p " +
       "LEFT JOIN p.boutique b " +
       "WHERE (b.entreprise.id = :entrepriseId OR b IS NULL) " +
       "AND (p.deleted IS NULL OR p.deleted = false)")
long countProduitsUniquesByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

// Compter le nombre total de boutiques actives d'une entreprise
@Query("SELECT COUNT(DISTINCT b.id) FROM Boutique b " +
       "WHERE b.entreprise.id = :entrepriseId AND b.actif = true")
long countBoutiquesActivesByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

// Récupérer les produits d'une boutique avec pagination
@Query("SELECT p FROM Produit p " +
       "LEFT JOIN FETCH p.categorie c " +
       "LEFT JOIN FETCH p.uniteDeMesure u " +
       "LEFT JOIN FETCH p.boutique b " +
       "WHERE p.boutique.id = :boutiqueId " +
       "AND (p.deleted IS NULL OR p.deleted = false) " +
       "ORDER BY p.nom ASC")
Page<Produit> findProduitsByBoutiqueIdPaginated(
    @Param("boutiqueId") Long boutiqueId, 
    Pageable pageable);

// Compter le nombre total de produits actifs d'une boutique
@Query("SELECT COUNT(p) FROM Produit p " +
       "WHERE p.boutique.id = :boutiqueId " +
       "AND (p.deleted IS NULL OR p.deleted = false)")
long countProduitsActifsByBoutiqueId(@Param("boutiqueId") Long boutiqueId);

// Compter le nombre de produits en stock d'une boutique
@Query("SELECT COUNT(p) FROM Produit p " +
       "WHERE p.boutique.id = :boutiqueId " +
       "AND (p.deleted IS NULL OR p.deleted = false) " +
       "AND p.enStock = true")
long countProduitsEnStockByBoutiqueId(@Param("boutiqueId") Long boutiqueId);

// Compter le nombre de produits hors stock d'une boutique
@Query("SELECT COUNT(p) FROM Produit p " +
       "WHERE p.boutique.id = :boutiqueId " +
       "AND (p.deleted IS NULL OR p.deleted = false) " +
       "AND (p.enStock = false OR p.enStock IS NULL)")
long countProduitsHorsStockByBoutiqueId(@Param("boutiqueId") Long boutiqueId);

}

