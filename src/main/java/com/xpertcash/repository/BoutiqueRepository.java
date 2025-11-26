package com.xpertcash.repository;

import java.util.List;
import java.util.Optional;

import com.xpertcash.entity.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Boutique;

@Repository
public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {
    List<Boutique> findByEntrepriseId(Long entrepriseId);

    Optional<Boutique> findByEntreprise(Entreprise entreprise);
    List<Boutique> findByEntrepriseIdAndActifTrue(Long entrepriseId);

    // Compter toutes les boutiques d'une entreprise
    long countByEntrepriseId(Long entrepriseId);

}

