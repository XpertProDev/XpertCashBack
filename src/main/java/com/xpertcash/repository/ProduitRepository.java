package com.xpertcash.repository;
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
    // Recherche par nom, prix et boutique (optimisé avec JOIN FETCH)
    @Query("SELECT p FROM Produit p " +
           "LEFT JOIN FETCH p.boutique b " +
           "WHERE p.nom = :nom AND p.prixVente = :prixVente AND b.id = :boutiqueId")
    Produit findByNomAndPrixVenteAndBoutiqueId(String nom, Double prixVente, Long boutiqueId);

    // Recherche par nom et boutique (optimisé avec JOIN FETCH)
    @Query("SELECT p FROM Produit p " +
           "LEFT JOIN FETCH p.boutique b " +
           "WHERE p.nom = :nom AND b.id = :boutiqueId")
    Produit findByNomAndBoutiqueId(String nom, Long boutiqueId);

    // Recherche par code-barres et boutique (optimisé avec JOIN FETCH)
    @Query("SELECT p FROM Produit p " +
           "LEFT JOIN FETCH p.boutique b " +
           "WHERE p.codeBare = :codeBare AND b.id = :boutiqueId")
    Produit findByCodeBareAndBoutiqueId(String codeBare, Long boutiqueId);

    List<Produit> findByBoutique(Boutique boutique);

    List<Produit> findByBoutiqueIdAndEnStockFalse(Long boutiqueId);

    List<Produit> findByBoutiqueIdAndEnStockTrue(Long boutiqueId);

    // Recherche par boutique et ID produit (optimisé avec JOIN FETCH)
    @Query("SELECT p FROM Produit p " +
           "LEFT JOIN FETCH p.boutique b " +
           "WHERE b.id = :boutiqueId AND p.id = :produitId")
    Optional<Produit> findByBoutiqueAndId(@Param("boutiqueId") Long boutiqueId, @Param("produitId") Long produitId);

    // Recherche par boutique et code générique (optimisé avec JOIN FETCH)
    @Query("SELECT p FROM Produit p " +
           "LEFT JOIN FETCH p.boutique b " +
           "WHERE b.id = :boutiqueId AND p.codeGenerique = :codeGenerique")
    Optional<Produit> findByBoutiqueAndCodeGenerique(@Param("boutiqueId") Long boutiqueId, @Param("codeGenerique") String codeGenerique);

    // Récupérer tous les produits d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT p FROM Produit p " +
           "LEFT JOIN FETCH p.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE e.id = :entrepriseId")
    List<Produit> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
    
    // Recherche par nom et entreprise (optimisé avec JOIN)
    @Query("SELECT p FROM Produit p " +
           "LEFT JOIN p.boutique b " +
           "LEFT JOIN b.entreprise e " +
           "WHERE p.nom = :nom AND e.id = :entrepriseId")
    Produit findByNomAndEntrepriseId(@Param("nom") String nom, @Param("entrepriseId") Long entrepriseId);

    // Recherche par code générique et entreprise (pour isolation)
    @Query("SELECT p FROM Produit p " +
           "LEFT JOIN FETCH p.boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE p.codeGenerique = :codeGenerique AND e.id = :entrepriseId")
    List<Produit> findByCodeGeneriqueAndEntrepriseId(@Param("codeGenerique") String codeGenerique, @Param("entrepriseId") Long entrepriseId);

    /** Page de codeGenerique distincts (tri par code) — pagination côté base. */
    @Query(value = """
        SELECT p.code_generique FROM produit p
        LEFT JOIN boutique b ON p.boutique_id = b.id
        WHERE (b.entreprise_id = :entrepriseId OR b.id IS NULL)
          AND (p.deleted IS NULL OR p.deleted = false)
        GROUP BY p.code_generique
        ORDER BY p.code_generique ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<String> findCodeGeneriquesPageOrderByCodeGenerique(
            @Param("entrepriseId") Long entrepriseId, @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = """
        SELECT p.code_generique FROM produit p
        LEFT JOIN boutique b ON p.boutique_id = b.id
        WHERE (b.entreprise_id = :entrepriseId OR b.id IS NULL)
          AND (p.deleted IS NULL OR p.deleted = false)
        GROUP BY p.code_generique
        ORDER BY p.code_generique DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<String> findCodeGeneriquesPageOrderByCodeGeneriqueDesc(
            @Param("entrepriseId") Long entrepriseId, @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = """
        SELECT p.code_generique FROM produit p
        LEFT JOIN boutique b ON p.boutique_id = b.id
        WHERE (b.entreprise_id = :entrepriseId OR b.id IS NULL)
          AND (p.deleted IS NULL OR p.deleted = false)
        GROUP BY p.code_generique
        ORDER BY MIN(p.nom) ASC, p.code_generique ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<String> findCodeGeneriquesPageOrderByNom(
            @Param("entrepriseId") Long entrepriseId, @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = """
        SELECT p.code_generique FROM produit p
        LEFT JOIN boutique b ON p.boutique_id = b.id
        WHERE (b.entreprise_id = :entrepriseId OR b.id IS NULL)
          AND (p.deleted IS NULL OR p.deleted = false)
        GROUP BY p.code_generique
        ORDER BY MIN(p.nom) DESC, p.code_generique ASC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<String> findCodeGeneriquesPageOrderByNomDesc(
            @Param("entrepriseId") Long entrepriseId, @Param("limit") int limit, @Param("offset") int offset);

    /** Charge les produits d'une entreprise par liste de codeGenerique (pagination produits uniques). */
    @Query("SELECT DISTINCT p FROM Produit p " +
           "LEFT JOIN FETCH p.boutique b " +
           "LEFT JOIN FETCH p.categorie c " +
           "LEFT JOIN FETCH p.uniteDeMesure u " +
           "LEFT JOIN b.entreprise e " +
           "WHERE ((e.id = :entrepriseId) OR (b IS NULL)) AND p.codeGenerique IN :codeGeneriques " +
           "AND (p.deleted IS NULL OR p.deleted = false)")
    List<Produit> findByEntrepriseIdAndCodeGeneriqueIn(
            @Param("entrepriseId") Long entrepriseId,
            @Param("codeGeneriques") List<String> codeGeneriques);

    // Recherche par boutique et liste d'IDs (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT p FROM Produit p " +
           "LEFT JOIN FETCH p.boutique b " +
           "WHERE b.id = :boutiqueId AND p.id IN :produitIds")
    List<Produit> findByBoutiqueAndIdIn(@Param("boutiqueId") Long boutiqueId, @Param("produitIds") List<Long> produitIds);

    // Pour les produits non supprimés et enStock = false
    List<Produit> findByBoutiqueIdAndEnStockFalseAndDeletedFalseOrDeletedIsNull(Long boutiqueId);
    
    // Pour les produits non supprimés et enStock = true
    List<Produit> findByBoutiqueIdAndEnStockTrueAndDeletedFalseOrDeletedIsNull(Long boutiqueId);

    List<Produit> findByBoutiqueIdAndDeletedTrue(Long boutiqueId);

    List<Produit> findByBoutiqueIdAndDeletedFalse(Long boutiqueId);


    
    // Récupérer les produits non supprimés d'une boutique (optimisé avec JOIN FETCH)
    @Query("SELECT p FROM Produit p " +
           "LEFT JOIN FETCH p.boutique b " +
           "WHERE b.id = :boutiqueId AND (p.deleted IS NULL OR p.deleted = false)")
    List<Produit> findByBoutiqueIdAndNotDeleted(@Param("boutiqueId") Long boutiqueId);

    List<Produit> findByBoutiqueIdAndDeletedFalseOrDeletedIsNull(Long boutiqueId);

     // Compter les produits par catégorie et entreprise (optimisé avec JOIN)
     @Query("SELECT COUNT(p) FROM Produit p " +
            "INNER JOIN p.boutique b " +
            "INNER JOIN b.entreprise e " +
            "INNER JOIN p.categorie c " +
            "WHERE c.id = :categorieId AND e.id = :entrepriseId")
      long countByCategorieIdAndEntrepriseId(@Param("categorieId") Long categorieId, @Param("entrepriseId") Long entrepriseId);

       // Récupérer tous les produits d'une entreprise (optimisé avec JOIN FETCH)
       @Query("SELECT DISTINCT p FROM Produit p " +
              "LEFT JOIN FETCH p.boutique b " +
              "LEFT JOIN FETCH b.entreprise e " +
              "WHERE e.id = :entrepriseId")
       List<Produit> findAllByEntrepriseId(@Param("entrepriseId") Long entrepriseId);



// Récupérer tous les produits d'une entreprise avec leurs catégories et boutiques
@Query("SELECT DISTINCT p FROM Produit p " +
       "JOIN FETCH p.categorie c " +
       "LEFT JOIN FETCH p.boutique b " +
       "LEFT JOIN b.entreprise e " +
       "WHERE (e.id = :entrepriseId) OR (p.boutique IS NULL)")
List<Produit> findAllWithCategorieAndBoutiqueByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

// Compter les produits par catégorie pour une entreprise (ULTRA-OPTIMISÉ)
@Query("SELECT p.categorie.id, COUNT(p) FROM Produit p " +
       "INNER JOIN p.boutique b " +
       "INNER JOIN b.entreprise e " +
       "WHERE e.id = :entrepriseId " +
       "AND (p.deleted IS NULL OR p.deleted = false) " +
       "AND p.categorie.id IS NOT NULL " +
       "GROUP BY p.categorie.id " +
       "ORDER BY COUNT(p) DESC")
List<Object[]> countProduitsParCategorie(@Param("entrepriseId") Long entrepriseId);

// Compter les produits par catégorie pour une entreprise (excluant les produits de type SERVICE)
@Query("SELECT p.categorie.id, COUNT(p) FROM Produit p " +
       "INNER JOIN p.boutique b " +
       "INNER JOIN b.entreprise e " +
       "WHERE e.id = :entrepriseId " +
       "AND (p.deleted IS NULL OR p.deleted = false) " +
       "AND p.categorie.id IS NOT NULL " +
       "AND (p.typeProduit IS NULL OR p.typeProduit != 'SERVICE') " +
       "GROUP BY p.categorie.id " +
       "ORDER BY COUNT(p) DESC")
List<Object[]> countProduitsParCategorieExcluantService(@Param("entrepriseId") Long entrepriseId);

// Compter les produits par catégorie pour une liste spécifique de catégories
@Query("SELECT p.categorie.id, COUNT(p) FROM Produit p " +
       "INNER JOIN p.boutique b " +
       "INNER JOIN b.entreprise e " +
       "WHERE e.id = :entrepriseId " +
       "AND p.categorie.id IN :categorieIds " +
       "GROUP BY p.categorie.id")
List<Object[]> countProduitsParCategorieIds(@Param("entrepriseId") Long entrepriseId, @Param("categorieIds") List<Long> categorieIds);

// Récupérer les produits par une liste de catégories pour une entreprise
@Query("SELECT DISTINCT p FROM Produit p " +
       "JOIN FETCH p.categorie c " +
       "LEFT JOIN FETCH p.boutique b " +
       "LEFT JOIN b.entreprise e " +
       "WHERE p.categorie.id IN :categorieIds " +
       "AND ((e.id = :entrepriseId) OR (b IS NULL))")
List<Produit> findByCategorieIdsAndEntrepriseId(@Param("categorieIds") List<Long> categorieIds, @Param("entrepriseId") Long entrepriseId);

// Récupérer les produits d'une catégorie spécifique avec pagination
@Query("SELECT DISTINCT p FROM Produit p " +
       "JOIN FETCH p.categorie c " +
       "LEFT JOIN FETCH p.boutique b " +
       "LEFT JOIN b.entreprise e " +
       "WHERE p.categorie.id = :categorieId " +
       "AND ((e.id = :entrepriseId) OR (b IS NULL))")
Page<Produit> findByCategorieIdAndEntrepriseIdPaginated(
    @Param("categorieId") Long categorieId, 
    @Param("entrepriseId") Long entrepriseId, 
    Pageable pageable);


// Récupérer tous les produits d'une entreprise avec la boutique jointe
@Query("SELECT DISTINCT p FROM Produit p " +
       "LEFT JOIN FETCH p.boutique b " +
       "LEFT JOIN b.entreprise e " +
       "WHERE (e.id = :entrepriseId) OR (b IS NULL)")
List<Produit> findAllWithBoutiqueByEntrepriseId(@Param("entrepriseId") Long entrepriseId);


// Récupérer tous les produits actifs d'une boutique avec les relations nécessaires
@Query("SELECT DISTINCT p FROM Produit p " +
       "LEFT JOIN FETCH p.boutique b " +
       "WHERE b.id = :boutiqueId AND (p.deleted IS NULL OR p.deleted = false)")
List<Produit> findActiveByBoutiqueIdWithRelations(@Param("boutiqueId") Long boutiqueId);

// Récupérer les produits d'une entreprise avec pagination (tri via Pageable)
@Query("SELECT DISTINCT p FROM Produit p " +
       "LEFT JOIN FETCH p.boutique b " +
       "LEFT JOIN b.entreprise e " +
       "WHERE ((e.id = :entrepriseId) OR (b IS NULL)) " +
       "AND (p.deleted IS NULL OR p.deleted = false)")
Page<Produit> findProduitsByEntrepriseIdPaginated(
    @Param("entrepriseId") Long entrepriseId,
    Pageable pageable);

// Compter le nombre total de produits uniques par entreprise
@Query("SELECT COUNT(DISTINCT p.codeGenerique) FROM Produit p " +
       "LEFT JOIN p.boutique b " +
       "LEFT JOIN b.entreprise e " +
       "WHERE ((e.id = :entrepriseId) OR (b IS NULL)) " +
       "AND (p.deleted IS NULL OR p.deleted = false)")
long countProduitsUniquesByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

// Compter le nombre total de boutiques actives d'une entreprise (optimisé avec JOIN)
@Query("SELECT COUNT(DISTINCT b.id) FROM Boutique b " +
       "INNER JOIN b.entreprise e " +
       "WHERE e.id = :entrepriseId AND b.actif = true")
long countBoutiquesActivesByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

// Récupérer les produits d'une boutique avec pagination
@Query("SELECT DISTINCT p FROM Produit p " +
       "LEFT JOIN FETCH p.categorie c " +
       "LEFT JOIN FETCH p.uniteDeMesure u " +
       "LEFT JOIN FETCH p.boutique b " +
       "WHERE b.id = :boutiqueId " +
       "AND (p.deleted IS NULL OR p.deleted = false) " +
       "ORDER BY p.nom ASC")
Page<Produit> findProduitsByBoutiqueIdPaginated(
    @Param("boutiqueId") Long boutiqueId, 
    Pageable pageable);

// Compter le nombre total de produits actifs d'une boutique (optimisé avec JOIN)
@Query("SELECT COUNT(p) FROM Produit p " +
       "INNER JOIN p.boutique b " +
       "WHERE b.id = :boutiqueId " +
       "AND (p.deleted IS NULL OR p.deleted = false)")
long countProduitsActifsByBoutiqueId(@Param("boutiqueId") Long boutiqueId);

// Compter le nombre de produits en stock d'une boutique (optimisé avec JOIN)
@Query("SELECT COUNT(p) FROM Produit p " +
       "INNER JOIN p.boutique b " +
       "WHERE b.id = :boutiqueId " +
       "AND (p.deleted IS NULL OR p.deleted = false) " +
       "AND p.enStock = true")
long countProduitsEnStockByBoutiqueId(@Param("boutiqueId") Long boutiqueId);

// Compter le nombre de produits hors stock d'une boutique (optimisé avec JOIN)
@Query("SELECT COUNT(p) FROM Produit p " +
       "INNER JOIN p.boutique b " +
       "WHERE b.id = :boutiqueId " +
       "AND (p.deleted IS NULL OR p.deleted = false) " +
       "AND (p.enStock = false OR p.enStock IS NULL)")
long countProduitsHorsStockByBoutiqueId(@Param("boutiqueId") Long boutiqueId);

// --- POS / Vente : produits en stock uniquement, pagination côté base (IDs puis chargement) ---
@Query("SELECT COUNT(p) FROM Produit p " +
       "INNER JOIN p.boutique b " +
       "WHERE b.id = :boutiqueId " +
       "AND (p.deleted IS NULL OR p.deleted = false) " +
       "AND p.enStock = true " +
       "AND ( :search IS NULL OR :search = '' OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.codeGenerique) LIKE LOWER(CONCAT('%', :search, '%')) )")
long countProduitsEnStockByBoutiqueIdForVenteWithSearch(
        @Param("boutiqueId") Long boutiqueId, @Param("search") String search);

@Query(value = "SELECT p.id FROM produit p WHERE p.boutique_id = :boutiqueId " +
        "AND (p.deleted IS NULL OR p.deleted = false) AND p.en_stock = true " +
        "AND (:search IS NULL OR :search = '' OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.code_generique) LIKE LOWER(CONCAT('%', :search, '%'))) " +
        "ORDER BY p.favori_pour_vente DESC, (p.ordre_favori IS NULL), p.ordre_favori ASC, p.nom ASC, p.id ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
List<Long> findProduitsEnStockIdsForVenteOrderByNomAsc(
        @Param("boutiqueId") Long boutiqueId, @Param("search") String search, @Param("limit") int limit, @Param("offset") int offset);
@Query(value = "SELECT p.id FROM produit p WHERE p.boutique_id = :boutiqueId " +
        "AND (p.deleted IS NULL OR p.deleted = false) AND p.en_stock = true " +
        "AND (:search IS NULL OR :search = '' OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.code_generique) LIKE LOWER(CONCAT('%', :search, '%'))) " +
        "ORDER BY p.favori_pour_vente DESC, (p.ordre_favori IS NULL), p.ordre_favori ASC, p.nom DESC, p.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
List<Long> findProduitsEnStockIdsForVenteOrderByNomDesc(
        @Param("boutiqueId") Long boutiqueId, @Param("search") String search, @Param("limit") int limit, @Param("offset") int offset);
@Query(value = "SELECT p.id FROM produit p WHERE p.boutique_id = :boutiqueId " +
        "AND (p.deleted IS NULL OR p.deleted = false) AND p.en_stock = true " +
        "AND (:search IS NULL OR :search = '' OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.code_generique) LIKE LOWER(CONCAT('%', :search, '%'))) " +
        "ORDER BY p.favori_pour_vente DESC, (p.ordre_favori IS NULL), p.ordre_favori ASC, p.created_at ASC, p.id ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
List<Long> findProduitsEnStockIdsForVenteOrderByCreatedAtAsc(
        @Param("boutiqueId") Long boutiqueId, @Param("search") String search, @Param("limit") int limit, @Param("offset") int offset);
@Query(value = "SELECT p.id FROM produit p WHERE p.boutique_id = :boutiqueId " +
        "AND (p.deleted IS NULL OR p.deleted = false) AND p.en_stock = true " +
        "AND (:search IS NULL OR :search = '' OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.code_generique) LIKE LOWER(CONCAT('%', :search, '%'))) " +
        "ORDER BY p.favori_pour_vente DESC, (p.ordre_favori IS NULL), p.ordre_favori ASC, p.created_at DESC, p.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
List<Long> findProduitsEnStockIdsForVenteOrderByCreatedAtDesc(
        @Param("boutiqueId") Long boutiqueId, @Param("search") String search, @Param("limit") int limit, @Param("offset") int offset);
@Query(value = "SELECT p.id FROM produit p WHERE p.boutique_id = :boutiqueId " +
        "AND (p.deleted IS NULL OR p.deleted = false) AND p.en_stock = true " +
        "AND (:search IS NULL OR :search = '' OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.code_generique) LIKE LOWER(CONCAT('%', :search, '%'))) " +
        "ORDER BY p.favori_pour_vente DESC, (p.ordre_favori IS NULL), p.ordre_favori ASC, p.prix_vente ASC, p.id ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
List<Long> findProduitsEnStockIdsForVenteOrderByPrixVenteAsc(
        @Param("boutiqueId") Long boutiqueId, @Param("search") String search, @Param("limit") int limit, @Param("offset") int offset);
@Query(value = "SELECT p.id FROM produit p WHERE p.boutique_id = :boutiqueId " +
        "AND (p.deleted IS NULL OR p.deleted = false) AND p.en_stock = true " +
        "AND (:search IS NULL OR :search = '' OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.code_generique) LIKE LOWER(CONCAT('%', :search, '%'))) " +
        "ORDER BY p.favori_pour_vente DESC, (p.ordre_favori IS NULL), p.ordre_favori ASC, p.prix_vente DESC, p.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
List<Long> findProduitsEnStockIdsForVenteOrderByPrixVenteDesc(
        @Param("boutiqueId") Long boutiqueId, @Param("search") String search, @Param("limit") int limit, @Param("offset") int offset);
@Query(value = "SELECT p.id FROM produit p WHERE p.boutique_id = :boutiqueId " +
        "AND (p.deleted IS NULL OR p.deleted = false) AND p.en_stock = true " +
        "AND (:search IS NULL OR :search = '' OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.code_generique) LIKE LOWER(CONCAT('%', :search, '%'))) " +
        "ORDER BY p.favori_pour_vente DESC, (p.ordre_favori IS NULL), p.ordre_favori ASC, p.code_generique ASC, p.id ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
List<Long> findProduitsEnStockIdsForVenteOrderByCodeGeneriqueAsc(
        @Param("boutiqueId") Long boutiqueId, @Param("search") String search, @Param("limit") int limit, @Param("offset") int offset);
@Query(value = "SELECT p.id FROM produit p WHERE p.boutique_id = :boutiqueId " +
        "AND (p.deleted IS NULL OR p.deleted = false) AND p.en_stock = true " +
        "AND (:search IS NULL OR :search = '' OR LOWER(p.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.code_generique) LIKE LOWER(CONCAT('%', :search, '%'))) " +
        "ORDER BY p.favori_pour_vente DESC, (p.ordre_favori IS NULL), p.ordre_favori ASC, p.code_generique DESC, p.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
List<Long> findProduitsEnStockIdsForVenteOrderByCodeGeneriqueDesc(
        @Param("boutiqueId") Long boutiqueId, @Param("search") String search, @Param("limit") int limit, @Param("offset") int offset);

/** Charge les produits par IDs avec relations pour POS (categorie, uniteDeMesure, boutique). */
@Query("SELECT DISTINCT p FROM Produit p " +
       "LEFT JOIN FETCH p.categorie c " +
       "LEFT JOIN FETCH p.uniteDeMesure u " +
       "LEFT JOIN FETCH p.boutique b " +
       "WHERE p.id IN :ids")
List<Produit> findByIdInWithDetailsForVente(@Param("ids") List<Long> ids);

}

