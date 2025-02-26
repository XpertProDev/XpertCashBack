package com.xpertcash.repository;

import com.xpertcash.entity.Boutique;
import com.xpertcash.entity.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {
    Optional<Boutique> findById(Long id);

    Optional<Boutique> findByNomBoutiqueAndEntreprise(String nomBoutique, Entreprise entreprise);

    List<Boutique> findByEntreprise(Entreprise entreprise);
}
