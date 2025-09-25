package com.xpertcash.repository.VENTE;

import org.springframework.data.jpa.repository.JpaRepository;
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
}