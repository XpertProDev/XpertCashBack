package com.xpertcash.repository.VENTE;

import com.xpertcash.entity.Caisse;
import com.xpertcash.entity.VENTE.StatutCaisse;

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

    Optional<Caisse> findTopByBoutiqueIdAndVendeurIdOrderByDateOuvertureDesc(Long boutiqueId, Long vendeurId);

    Optional<Caisse> findFirstByBoutiqueIdAndVendeurIdOrderByDateOuvertureDesc(Long boutiqueId, Long vendeurId);

    Optional<Caisse> findByVendeurIdAndStatut(Long vendeurId, StatutCaisse statut);
    Optional<Caisse> findByIdAndStatutAndBoutiqueId(Long caisseId, StatutCaisse statut, Long boutiqueId);
    Optional<Caisse> findByVendeurIdAndStatutAndBoutiqueId(Long vendeurId, StatutCaisse statut, Long boutiqueId);

    List<Caisse> findByVendeurIdAndBoutiqueId(Long vendeurId, Long boutiqueId);

}