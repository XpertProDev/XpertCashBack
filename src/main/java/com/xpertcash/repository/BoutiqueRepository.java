package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import com.xpertcash.entity.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Boutique;

@Repository
public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {
    
    // Récupérer toutes les boutiques d'une entreprise (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT b FROM Boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE e.id = :entrepriseId")
    List<Boutique> findByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Récupérer une boutique par entreprise (objet) - moins optimal, préférer findByEntrepriseId
    @Query("SELECT b FROM Boutique b " +
           "INNER JOIN b.entreprise e " +
           "WHERE e.id = :entrepriseId")
    Optional<Boutique> findByEntreprise(@Param("entrepriseId") Long entrepriseId);
    
    // Récupérer toutes les boutiques actives d'une entreprise (optimisé)
    @Query("SELECT DISTINCT b FROM Boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE e.id = :entrepriseId AND b.actif = true")
    List<Boutique> findByEntrepriseIdAndActifTrue(@Param("entrepriseId") Long entrepriseId);

    // Compter toutes les boutiques d'une entreprise (optimisé avec JOIN)
    @Query("SELECT COUNT(b) FROM Boutique b " +
           "INNER JOIN b.entreprise e " +
           "WHERE e.id = :entrepriseId")
    long countByEntrepriseId(@Param("entrepriseId") Long entrepriseId);
    
    // Récupérer une boutique par ID avec vérification d'entreprise (pour isolation)
    @Query("SELECT b FROM Boutique b " +
           "LEFT JOIN FETCH b.entreprise e " +
           "WHERE b.id = :boutiqueId AND e.id = :entrepriseId")
    Optional<Boutique> findByIdAndEntrepriseId(@Param("boutiqueId") Long boutiqueId, @Param("entrepriseId") Long entrepriseId);

}

