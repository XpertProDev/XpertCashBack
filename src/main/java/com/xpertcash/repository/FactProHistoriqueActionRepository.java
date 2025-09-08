package com.xpertcash.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.xpertcash.entity.FactProHistoriqueAction;
import com.xpertcash.entity.FactureProForma;

@Repository
public interface FactProHistoriqueActionRepository extends JpaRepository<FactProHistoriqueAction, Long> {

    List<FactProHistoriqueAction> findByFactureIdOrderByDateActionAsc(Long factureId);

    // Tri descendant (nouvelle méthode)
    List<FactProHistoriqueAction> findByFactureIdOrderByDateActionDesc(Long factureId);

   void deleteByFacture(FactureProForma facture);

    // Méthode pour insérer directement avec les IDs (évite les références circulaires)
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO fact_pro_historique_action (action, date_action, details, facture_id, montant_facture, utilisateur_id) VALUES (:action, :dateAction, :details, :factureId, :montantFacture, :utilisateurId)", nativeQuery = true)
    void insertHistoriqueAction(@Param("action") String action, 
                               @Param("dateAction") LocalDateTime dateAction, 
                               @Param("details") String details, 
                               @Param("factureId") Long factureId, 
                               @Param("montantFacture") BigDecimal montantFacture, 
                               @Param("utilisateurId") Long utilisateurId);

}
