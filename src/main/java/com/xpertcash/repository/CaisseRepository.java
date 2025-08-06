package com.xpertcash.repository;

import com.xpertcash.entity.Caisse;
import com.xpertcash.entity.StatutCaisse;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CaisseRepository extends JpaRepository<Caisse, Long> {

    List<Caisse> findByBoutiqueIdAndStatut(Long boutiqueId, StatutCaisse statut);

    boolean existsByBoutiqueIdAndVendeurIdAndStatut(Long boutiqueId, Long vendeurId, StatutCaisse statut);

    Optional<Caisse> findByIdAndStatut(Long id, StatutCaisse statut);

    Optional<Caisse> findFirstByBoutiqueIdAndVendeurIdAndStatut(Long boutiqueId, Long vendeurId, StatutCaisse statut);

    List<Caisse> findByBoutiqueId(Long boutiqueId);
    List<Caisse> findByVendeurId(Long vendeurId);


}