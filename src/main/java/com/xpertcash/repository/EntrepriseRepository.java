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

    Optional<Entreprise> findByNomEntreprise(String nom);
    boolean existsByIdentifiantEntreprise(String identifiantEntreprise);


    List<Entreprise> findAll();

    // Récupérer toutes les entreprises en excluant une entreprise par son nom (pour SUPER_ADMIN)
    @Query("SELECT e FROM Entreprise e WHERE e.nomEntreprise <> :excludedName")
    Page<Entreprise> findAllExcludingNomEntreprise(@Param("excludedName") String excludedName, Pageable pageable);
    


    //boolean existsByNomEntreprise(String nom);
}
