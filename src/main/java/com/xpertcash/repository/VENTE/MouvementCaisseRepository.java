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

    // Récupère les mouvements pour plusieurs caisses sans filtre de date
    List<MouvementCaisse> findByCaisseIdInAndTypeMouvement(
        List<Long> caisseIds, TypeMouvementCaisse typeMouvement);

    /** Charge les dépenses pour plusieurs caisses en une requête (évite N+1). */
    @Query("SELECT DISTINCT m FROM MouvementCaisse m JOIN FETCH m.caisse c LEFT JOIN FETCH c.vendeur LEFT JOIN FETCH c.boutique WHERE m.caisse.id IN :caisseIds AND m.typeMouvement = :type")
    List<MouvementCaisse> findByCaisseIdInAndTypeMouvementWithCaisseAndVendeurAndBoutique(
        @Param("caisseIds") List<Long> caisseIds, @Param("type") TypeMouvementCaisse typeMouvement);

    // Récupère tous les mouvements d'une caisse (tous types confondus)
    List<MouvementCaisse> findByCaisseId(Long caisseId);
}