package com.xpertcash.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Fournisseur;


@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, Long>{

    // Méthodes isolées par entreprise (optimisées avec JOIN FETCH)
    
    // Récupérer tous les fournisseurs d'une entreprise (liste complète, sans pagination)
    @Query("SELECT DISTINCT f FROM Fournisseur f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId")
    List<Fournisseur> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Pagination simple par entreprise (sans FETCH pour laisser JPA gérer la page)
    Page<Fournisseur> findByEntreprise_Id(Long entrepriseId, Pageable pageable);

    // Pagination + recherche (nom, société, email, téléphone) pour une entreprise
    @Query(
        value = "SELECT f FROM Fournisseur f " +
                "JOIN f.entreprise e " +
                "WHERE e.id = :entrepriseId AND (" +
                "LOWER(COALESCE(f.nomComplet, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                "LOWER(COALESCE(f.nomSociete, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                "LOWER(COALESCE(f.email, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                "COALESCE(f.telephone, '') LIKE CONCAT('%', :search, '%')" +
                ")",
        countQuery = "SELECT COUNT(f) FROM Fournisseur f " +
                     "JOIN f.entreprise e " +
                     "WHERE e.id = :entrepriseId AND (" +
                     "LOWER(COALESCE(f.nomComplet, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "LOWER(COALESCE(f.nomSociete, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "LOWER(COALESCE(f.email, '')) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                     "COALESCE(f.telephone, '') LIKE CONCAT('%', :search, '%')" +
                     ")"
    )
    Page<Fournisseur> findByEntrepriseIdWithSearch(
            @Param("entrepriseId") Long entrepriseId,
            @Param("search") String search,
            Pageable pageable);

    // Recherche par ID et entreprise (pour isolation)
    @Query("SELECT DISTINCT f FROM Fournisseur f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE f.id = :id AND e.id = :entrepriseId")
    Optional<Fournisseur> findByIdAndEntrepriseId(
            @Param("id") Long id,
            @Param("entrepriseId") Long entrepriseId);

    // Recherche par email et entreprise
    @Query("SELECT DISTINCT f FROM Fournisseur f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE f.email = :email AND e.id = :entrepriseId")
    Optional<Fournisseur> findByEmailAndEntrepriseId(
            @Param("email") String email,
            @Param("entrepriseId") Long entrepriseId);

    // Recherche par téléphone et entreprise
    @Query("SELECT DISTINCT f FROM Fournisseur f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE f.telephone = :telephone AND e.id = :entrepriseId")
    Optional<Fournisseur> findByTelephoneAndEntrepriseId(
            @Param("telephone") String telephone,
            @Param("entrepriseId") Long entrepriseId);

    // Recherche par email ou téléphone et entreprise
    @Query("SELECT DISTINCT f FROM Fournisseur f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE e.id = :entrepriseId " +
           "AND (f.email = :email OR f.telephone = :telephone)")
    Optional<Fournisseur> findByEmailOrTelephoneAndEntrepriseId(
            @Param("email") String email,
            @Param("telephone") String telephone,
            @Param("entrepriseId") Long entrepriseId);

    // Recherche par nom complet et entreprise
    @Query("SELECT DISTINCT f FROM Fournisseur f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "WHERE f.nomComplet = :nomComplet AND e.id = :entrepriseId")
    Optional<Fournisseur> findByNomCompletAndEntrepriseId(
            @Param("nomComplet") String nomComplet,
            @Param("entrepriseId") Long entrepriseId);

    // Vérifications d'existence (déjà isolées)
    boolean existsByEntrepriseIdAndEmailAndIdNot(Long entrepriseId, String email, Long id);
    boolean existsByEntrepriseIdAndPaysAndTelephoneAndIdNot(Long entrepriseId, String pays, String telephone, Long id);

}
