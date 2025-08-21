package com.xpertcash.repository.VENTE;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.entity.VENTE.Vente;
import com.xpertcash.entity.VENTE.VenteHistorique;

public interface VenteRepository extends JpaRepository<Vente, Long> {

    // Récupère toutes les ventes d'une boutique donnée
    List<Vente> findByBoutiqueId(Long boutiqueId);

    // Optionnel : si tu veux aussi filtrer par date
    @Query("SELECT v FROM Vente v WHERE v.boutique.id = :boutiqueId AND v.dateVente BETWEEN :startDate AND :endDate")
    List<Vente> findByBoutiqueIdAndDateRange(
            @Param("boutiqueId") Long boutiqueId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    List<Vente> findByVendeurId(Long vendeurId);

    // Récupère toutes les ventes d'un vendeur
    List<Vente> findByVendeur(User vendeur);

    // Si besoin : récupérer les ventes d'un vendeur pour une entreprise spécifique
    List<Vente> findByVendeurAndBoutique_Entreprise(User vendeur, Entreprise entreprise);

    List<Vente> findByBoutique_Entreprise_IdAndDateVenteBetween(
            Long entrepriseId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay
    );
        
    List<Vente> findByBoutiqueEntrepriseId(Long entrepriseId);
    


}