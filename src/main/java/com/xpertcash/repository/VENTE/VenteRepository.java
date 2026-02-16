package com.xpertcash.repository.VENTE;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.xpertcash.entity.Entreprise;
import com.xpertcash.entity.User;
import com.xpertcash.entity.ModePaiement;
import com.xpertcash.entity.VENTE.Vente;

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
    
    // Récupère les ventes d'un vendeur spécifique dans une période
    List<Vente> findByVendeur_IdAndDateVenteBetween(
            Long vendeurId,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
        
    List<Vente> findByBoutiqueEntrepriseId(Long entrepriseId);

   List<Vente> findByClientId(Long clientId);
   List<Vente> findByEntrepriseClientId(Long entrepriseClientId);

   //findAllByEntrepriseId
   
    // Récupère toutes les ventes des boutiques d'une entreprise
    @Query("SELECT v FROM Vente v WHERE v.boutique.entreprise.id = :entrepriseId")
    List<Vente> findAllByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Récupère les ventes récentes d'une entreprise (triées par date décroissante)
    @Query("SELECT v FROM Vente v WHERE v.boutique.entreprise.id = :entrepriseId ORDER BY v.dateVente DESC")
    List<Vente> findRecentVentesByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Récupère les ventes effectuées avec des caisses fermées
    @Query("SELECT v FROM Vente v WHERE v.boutique.entreprise.id = :entrepriseId AND v.caisse.statut = 'FERMEE' ORDER BY v.dateVente DESC")
    List<Vente> findByEntrepriseIdAndCaisseFermee(@Param("entrepriseId") Long entrepriseId);

    // Compter toutes les ventes des boutiques d'une entreprise
    @Query("SELECT COUNT(v) FROM Vente v WHERE v.boutique.entreprise.id = :entrepriseId")
    long countByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Ventes à crédit (dette clients) pour une entreprise
    List<Vente> findByBoutique_Entreprise_IdAndModePaiement(Long entrepriseId, ModePaiement modePaiement);

    // Statistiques globales combinées (COUNT et SUM en une seule requête)
    // Retourne une liste avec une seule ligne : [0] = totalVentes (Long), [1] = montantTotal (Double)
    @Query("SELECT COUNT(v), COALESCE(SUM(v.montantTotal), 0) FROM Vente v " +
           "WHERE v.boutique.entreprise.id = :entrepriseId " +
           "AND v.dateVente >= :dateDebut AND v.dateVente < :dateFin")
    List<Object[]> getStatistiquesGlobalesByEntrepriseIdAndPeriode(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    // Même statistiques avec filtres optionnels vendeur et boutique
    @Query(value = "SELECT COUNT(v.id), COALESCE(SUM(v.montant_total), 0) " +
           "FROM vente v INNER JOIN boutique b ON v.boutique_id = b.id " +
           "WHERE b.entreprise_id = :entrepriseId " +
           "AND v.date_vente >= :dateDebut AND v.date_vente < :dateFin " +
           "AND (:vendeurId IS NULL OR v.vendeur_id = :vendeurId) " +
           "AND (:boutiqueId IS NULL OR v.boutique_id = :boutiqueId)", nativeQuery = true)
    List<Object[]> getStatistiquesGlobalesByEntrepriseIdAndPeriodeAndFilters(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("vendeurId") Long vendeurId,
            @Param("boutiqueId") Long boutiqueId);

    // Top 3 vendeurs par entreprise et période
    @Query(value = "SELECT v.vendeur_id, u.nom_complet, COUNT(v.id) as nombre_ventes, " +
           "COALESCE(SUM(v.montant_total), 0) as montant_total " +
           "FROM vente v " +
           "INNER JOIN boutique b ON v.boutique_id = b.id " +
           "INNER JOIN user u ON v.vendeur_id = u.id " +
           "WHERE b.entreprise_id = :entrepriseId " +
           "AND v.date_vente >= :dateDebut AND v.date_vente < :dateFin " +
           "AND v.vendeur_id IS NOT NULL " +
           "GROUP BY v.vendeur_id, u.nom_complet " +
           "ORDER BY montant_total DESC " +
           "LIMIT 3", nativeQuery = true)
    List<Object[]> findTop3VendeursByEntrepriseIdAndPeriode(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    // TOUS les vendeurs par entreprise et période (triés par montant décroissant)
    @Query(value = "SELECT v.vendeur_id, u.nom_complet, COUNT(v.id) as nombre_ventes, " +
           "COALESCE(SUM(v.montant_total), 0) as montant_total " +
           "FROM vente v " +
           "INNER JOIN boutique b ON v.boutique_id = b.id " +
           "INNER JOIN user u ON v.vendeur_id = u.id " +
           "WHERE b.entreprise_id = :entrepriseId " +
           "AND v.date_vente >= :dateDebut AND v.date_vente < :dateFin " +
           "AND v.vendeur_id IS NOT NULL " +
           "GROUP BY v.vendeur_id, u.nom_complet " +
           "ORDER BY montant_total DESC", nativeQuery = true)
    List<Object[]> findAllVendeursByEntrepriseIdAndPeriode(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    // Même avec filtres optionnels vendeur et boutique
    @Query(value = "SELECT v.vendeur_id, u.nom_complet, COUNT(v.id) as nombre_ventes, " +
           "COALESCE(SUM(v.montant_total), 0) as montant_total " +
           "FROM vente v " +
           "INNER JOIN boutique b ON v.boutique_id = b.id " +
           "INNER JOIN user u ON v.vendeur_id = u.id " +
           "WHERE b.entreprise_id = :entrepriseId " +
           "AND v.date_vente >= :dateDebut AND v.date_vente < :dateFin " +
           "AND v.vendeur_id IS NOT NULL " +
           "AND (:vendeurId IS NULL OR v.vendeur_id = :vendeurId) " +
           "AND (:boutiqueId IS NULL OR v.boutique_id = :boutiqueId) " +
           "GROUP BY v.vendeur_id, u.nom_complet " +
           "ORDER BY montant_total DESC", nativeQuery = true)
    List<Object[]> findAllVendeursByEntrepriseIdAndPeriodeAndFilters(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("vendeurId") Long vendeurId,
            @Param("boutiqueId") Long boutiqueId);

    // Montants des ventes par statut de caisse (OUVERTE et FERMEE) en une seule requête
    // Retourne: [0] = montantCaisseOuverte, [1] = montantCaisseFermee
    @Query(value = "SELECT " +
           "COALESCE(SUM(CASE WHEN c.statut = 'OUVERTE' THEN v.montant_total ELSE 0 END), 0) as montant_ouverte, " +
           "COALESCE(SUM(CASE WHEN c.statut = 'FERMEE' THEN v.montant_total ELSE 0 END), 0) as montant_fermee " +
           "FROM vente v " +
           "INNER JOIN boutique b ON v.boutique_id = b.id " +
           "INNER JOIN caisse c ON v.caisse_id = c.id " +
           "WHERE b.entreprise_id = :entrepriseId " +
           "AND v.date_vente >= :dateDebut AND v.date_vente < :dateFin", nativeQuery = true)
    List<Object[]> sumMontantParStatutCaisseByEntrepriseIdAndPeriode(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    @Query(value = "SELECT " +
           "COALESCE(SUM(CASE WHEN c.statut = 'OUVERTE' THEN v.montant_total ELSE 0 END), 0) as montant_ouverte, " +
           "COALESCE(SUM(CASE WHEN c.statut = 'FERMEE' THEN v.montant_total ELSE 0 END), 0) as montant_fermee " +
           "FROM vente v " +
           "INNER JOIN boutique b ON v.boutique_id = b.id " +
           "INNER JOIN caisse c ON v.caisse_id = c.id " +
           "WHERE b.entreprise_id = :entrepriseId " +
           "AND v.date_vente >= :dateDebut AND v.date_vente < :dateFin " +
           "AND (:vendeurId IS NULL OR v.vendeur_id = :vendeurId) " +
           "AND (:boutiqueId IS NULL OR v.boutique_id = :boutiqueId)", nativeQuery = true)
    List<Object[]> sumMontantParStatutCaisseByEntrepriseIdAndPeriodeAndFilters(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("vendeurId") Long vendeurId,
            @Param("boutiqueId") Long boutiqueId);

    // ==================== STATISTIQUES PAR VENDEUR ====================

    // Statistiques d'un vendeur spécifique (COUNT et SUM en une seule requête)
    // Retourne: [0] = totalVentes (Long), [1] = montantTotal (Double)
    @Query(value = "SELECT COUNT(v.id), COALESCE(SUM(v.montant_total), 0) " +
           "FROM vente v " +
           "INNER JOIN boutique b ON v.boutique_id = b.id " +
           "WHERE b.entreprise_id = :entrepriseId " +
           "AND v.vendeur_id = :vendeurId " +
           "AND v.date_vente >= :dateDebut AND v.date_vente < :dateFin", nativeQuery = true)
    List<Object[]> getStatistiquesVendeurByEntrepriseIdAndPeriode(
            @Param("entrepriseId") Long entrepriseId,
            @Param("vendeurId") Long vendeurId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    // Montants des ventes d'un vendeur par statut de caisse (OUVERTE et FERMEE)
    // Retourne: [0] = montantCaisseOuverte, [1] = montantCaisseFermee
    @Query(value = "SELECT " +
           "COALESCE(SUM(CASE WHEN c.statut = 'OUVERTE' THEN v.montant_total ELSE 0 END), 0) as montant_ouverte, " +
           "COALESCE(SUM(CASE WHEN c.statut = 'FERMEE' THEN v.montant_total ELSE 0 END), 0) as montant_fermee " +
           "FROM vente v " +
           "INNER JOIN boutique b ON v.boutique_id = b.id " +
           "INNER JOIN caisse c ON v.caisse_id = c.id " +
           "WHERE b.entreprise_id = :entrepriseId " +
           "AND v.vendeur_id = :vendeurId " +
           "AND v.date_vente >= :dateDebut AND v.date_vente < :dateFin", nativeQuery = true)
    List<Object[]> sumMontantParStatutCaisseByVendeurAndPeriode(
            @Param("entrepriseId") Long entrepriseId,
            @Param("vendeurId") Long vendeurId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

}