package com.xpertcash.repository;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.Magasin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MagasinRepository extends JpaRepository<Magasin, Long> {
    List<Magasin> findByEntreprise(Entreprise entreprise);
    Optional<Magasin> findByNomMagasinAndEntreprise(String nomMagasin, Entreprise entreprise);
    Optional<Magasin> findByNomMagasin(String nomMagasin);
}

