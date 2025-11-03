package com.xpertcash.repository.VENTE;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

import com.xpertcash.entity.VENTE.MouvementCaisse;
import com.xpertcash.entity.VENTE.TypeMouvementCaisse;

public interface MouvementCaisseRepository extends JpaRepository<MouvementCaisse, Long> {
    
    List<MouvementCaisse> findByCaisseIdAndTypeMouvement(Long caisseId, TypeMouvementCaisse typeMouvement);
    
    // Méthodes pour le résumé des transactions
    List<MouvementCaisse> findByCaisse_Boutique_Entreprise_IdAndTypeMouvementAndDateMouvementBetween(
        Long entrepriseId, TypeMouvementCaisse typeMouvement, LocalDateTime dateDebut, LocalDateTime dateFin);
    
    // Récupère les mouvements pour plusieurs caisses
    List<MouvementCaisse> findByCaisseIdInAndTypeMouvementAndDateMouvementBetween(
        List<Long> caisseIds, TypeMouvementCaisse typeMouvement, LocalDateTime dateDebut, LocalDateTime dateFin);
    
    // Récupère tous les mouvements d'un type pour une entreprise
    @Query("SELECT m FROM MouvementCaisse m WHERE m.caisse.boutique.entreprise.id = :entrepriseId AND m.typeMouvement = :typeMouvement")
    List<MouvementCaisse> findByCaisse_Boutique_Entreprise_IdAndTypeMouvement(
        @Param("entrepriseId") Long entrepriseId, @Param("typeMouvement") TypeMouvementCaisse typeMouvement);
}