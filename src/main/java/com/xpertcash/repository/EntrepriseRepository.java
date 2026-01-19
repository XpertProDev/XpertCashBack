package com.xpertcash.repository;

import com.xpertcash.entity.Entreprise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {

    // Recherche par nom d'entreprise (optimisé avec JOIN FETCH pour admin et modules)
    @Query("SELECT e FROM Entreprise e " +
           "LEFT JOIN FETCH e.admin " +
           "LEFT JOIN FETCH e.modulesActifs " +
           "WHERE e.nomEntreprise = :nom")
    Optional<Entreprise> findByNomEntreprise(@Param("nom") String nom);
    
    // Vérifier l'existence par identifiant (optimisé avec JOIN pour comptage)
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Entreprise e " +
           "WHERE e.identifiantEntreprise = :identifiantEntreprise")
    boolean existsByIdentifiantEntreprise(@Param("identifiantEntreprise") String identifiantEntreprise);

    // Récupérer toutes les entreprises avec relations (optimisé avec JOIN FETCH pour admin et modules)
    // Utilisé principalement par SUPER_ADMIN et lors de l'initialisation
    @Query("SELECT DISTINCT e FROM Entreprise e " +
           "LEFT JOIN FETCH e.admin " +
           "LEFT JOIN FETCH e.modulesActifs")
    List<Entreprise> findAllWithRelations();

    // Récupérer toutes les entreprises en excluant une entreprise par son nom (pour SUPER_ADMIN)
    // Optimisé avec JOIN FETCH pour charger admin et modules en une requête
    @Query("SELECT DISTINCT e FROM Entreprise e " +
           "LEFT JOIN FETCH e.admin " +
           "LEFT JOIN FETCH e.modulesActifs " +
           "WHERE e.nomEntreprise <> :excludedName")
    Page<Entreprise> findAllExcludingNomEntreprise(@Param("excludedName") String excludedName, Pageable pageable);
}
