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

    // Récupérer l'historique d'une facture trié par date croissante (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT h FROM FactProHistoriqueAction h " +
           "LEFT JOIN FETCH h.facture f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH h.utilisateur u " +
           "WHERE h.facture.id = :factureId " +
           "ORDER BY h.dateAction ASC")
    List<FactProHistoriqueAction> findByFactureIdOrderByDateActionAsc(@Param("factureId") Long factureId);

    // Récupérer l'historique d'une facture trié par date décroissante (optimisé avec JOIN FETCH)
    @Query("SELECT DISTINCT h FROM FactProHistoriqueAction h " +
           "LEFT JOIN FETCH h.facture f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH h.utilisateur u " +
           "WHERE h.facture.id = :factureId " +
           "ORDER BY h.dateAction DESC")
    List<FactProHistoriqueAction> findByFactureIdOrderByDateActionDesc(@Param("factureId") Long factureId);

    // Récupérer l'historique d'une entreprise (pour isolation)
    @Query("SELECT DISTINCT h FROM FactProHistoriqueAction h " +
           "LEFT JOIN FETCH h.facture f " +
           "LEFT JOIN FETCH f.entreprise e " +
           "LEFT JOIN FETCH h.utilisateur u " +
           "WHERE e.id = :entrepriseId " +
           "ORDER BY h.dateAction DESC")
    List<FactProHistoriqueAction> findByEntrepriseIdOrderByDateActionDesc(@Param("entrepriseId") Long entrepriseId);

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
