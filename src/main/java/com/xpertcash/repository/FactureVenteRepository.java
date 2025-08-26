package com.xpertcash.repository;

import com.xpertcash.entity.FactureVente;
import com.xpertcash.entity.VENTE.Vente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactureVenteRepository extends JpaRepository<FactureVente, Long> {
    @Query("SELECT f FROM FactureVente f WHERE f.vente.boutique.entreprise.id = :entrepriseId")
    List<FactureVente> findAllByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    Optional<FactureVente> findByVente(Vente vente);

}