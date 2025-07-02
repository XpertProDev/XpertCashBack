package com.xpertcash.repository.Module;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Module.AppModule;
import com.xpertcash.entity.Module.EntrepriseModuleEssai;

@Repository
public interface EntrepriseModuleEssaiRepository extends JpaRepository<EntrepriseModuleEssai, Long> {

    // Liste des essais pour une entreprise
    List<EntrepriseModuleEssai> findByEntreprise(Entreprise entreprise);

    // Recherche d'un essai précis pour une entreprise et un module
    Optional<EntrepriseModuleEssai> findByEntrepriseAndModule(Entreprise entreprise, AppModule module);

    // Vérifie si un essai existe pour un module et une entreprise (utile pour éviter les doublons)
    boolean existsByEntrepriseAndModule(Entreprise entreprise, AppModule module);

    // Supprime tous les essais d'une entreprise (utile si l'entreprise achète le module ou à la fin d'essai)
    void deleteByEntreprise(Entreprise entreprise);



}

