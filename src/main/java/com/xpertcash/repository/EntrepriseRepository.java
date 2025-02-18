package com.xpertcash.repository;

import com.xpertcash.entity.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {

    Optional<Entreprise> findByNomEntreprise(String nom);
    boolean existsByIdentifiantEntreprise(String identifiantEntreprise);


    List<Entreprise> findAll();
    


    //boolean existsByNomEntreprise(String nom);
}
