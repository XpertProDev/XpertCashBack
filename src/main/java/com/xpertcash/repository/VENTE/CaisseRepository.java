package com.xpertcash.repository.VENTE;

import com.xpertcash.entity.Caisse;
import com.xpertcash.entity.VENTE.StatutCaisse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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
    
    List<Caisse> findByVendeur_Id(Long vendeurId);

    @Query("SELECT c FROM Caisse c WHERE c.boutique.entreprise.id = :entrepriseId AND c.statut = :statut")
    List<Caisse> findByEntrepriseIdAndStatut(@Param("entrepriseId") Long entrepriseId, @Param("statut") StatutCaisse statut);

    @Query("SELECT c FROM Caisse c WHERE c.boutique.entreprise.id = :entrepriseId AND c.statut = :statut ORDER BY c.dateFermeture DESC")
    List<Caisse> findByEntrepriseIdAndStatutOrderByDateFermetureDesc(@Param("entrepriseId") Long entrepriseId, @Param("statut") StatutCaisse statut);

    // Compter les caisses par entreprise et statut
    @Query("SELECT COUNT(c) FROM Caisse c WHERE c.boutique.entreprise.id = :entrepriseId AND c.statut = :statut")
    long countByEntrepriseIdAndStatut(@Param("entrepriseId") Long entrepriseId, @Param("statut") StatutCaisse statut);

}
