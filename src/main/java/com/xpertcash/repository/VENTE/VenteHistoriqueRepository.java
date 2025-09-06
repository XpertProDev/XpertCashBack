package com.xpertcash.repository.VENTE;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xpertcash.entity.VENTE.Vente;
import com.xpertcash.entity.VENTE.VenteHistorique;

public interface VenteHistoriqueRepository extends JpaRepository<VenteHistorique, Long> {
    List<VenteHistorique> findByVenteId(Long venteId);

     List<VenteHistorique> findByVenteAndAction(Vente vente, String action);
     
     // Optimisation N+1 : Récupérer tous les remboursements pour plusieurs ventes
     @Query("SELECT vh.vente.id, COALESCE(SUM(vh.montant), 0) FROM VenteHistorique vh " +
            "WHERE vh.vente.id IN :venteIds AND vh.action = 'REMBOURSEMENT_VENTE' " +
            "GROUP BY vh.vente.id")
     List<Object[]> sumRemboursementsByVenteIds(@Param("venteIds") List<Long> venteIds);
     
}