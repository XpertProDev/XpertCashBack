package com.xpertcash.repository.VENTE;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /** Pagination côté DB : ventes d'un vendeur avec relations chargées (évite N+1). */
    @Query(value = "SELECT DISTINCT v FROM Vente v " +
            "LEFT JOIN FETCH v.boutique " +
            "LEFT JOIN FETCH v.vendeur " +
            "LEFT JOIN FETCH v.caisse " +
            "LEFT JOIN FETCH v.client vclient " +
            "LEFT JOIN FETCH v.entrepriseClient vec " +
            "WHERE v.vendeur.id = :vendeurId",
            countQuery = "SELECT COUNT(v) FROM Vente v WHERE v.vendeur.id = :vendeurId")
    Page<Vente> findByVendeurIdPaginated(@Param("vendeurId") Long vendeurId, Pageable pageable);

    /** Pagination + recherche : par nom client (clientNom, clientNumero, client.nomComplet, entrepriseClient.nom) ou numéro facture. */
    @Query(value = "SELECT DISTINCT v FROM Vente v " +
            "LEFT JOIN FETCH v.boutique " +
            "LEFT JOIN FETCH v.vendeur " +
            "LEFT JOIN FETCH v.caisse " +
            "LEFT JOIN FETCH v.client vclient " +
            "LEFT JOIN FETCH v.entrepriseClient vec " +
            "WHERE v.vendeur.id = :vendeurId " +
            "AND (LOWER(COALESCE(v.clientNom, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')) " +
            "     OR LOWER(COALESCE(v.clientNumero, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')) " +
            "     OR (vclient IS NOT NULL AND LOWER(COALESCE(vclient.nomComplet, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
            "     OR (vec IS NOT NULL AND LOWER(COALESCE(vec.nom, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
            "     OR EXISTS (SELECT 1 FROM com.xpertcash.entity.FactureVente fv WHERE fv.vente.id = v.id AND LOWER(COALESCE(fv.numeroFacture, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))))",
            countQuery = "SELECT COUNT(DISTINCT v) FROM Vente v " +
            "LEFT JOIN v.client vclient " +
            "LEFT JOIN v.entrepriseClient vec " +
            "WHERE v.vendeur.id = :vendeurId " +
            "AND (LOWER(COALESCE(v.clientNom, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')) " +
            "     OR LOWER(COALESCE(v.clientNumero, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%')) " +
            "     OR (vclient IS NOT NULL AND LOWER(COALESCE(vclient.nomComplet, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
            "     OR (vec IS NOT NULL AND LOWER(COALESCE(vec.nom, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))) " +
            "     OR EXISTS (SELECT 1 FROM com.xpertcash.entity.FactureVente fv WHERE fv.vente.id = v.id AND LOWER(COALESCE(fv.numeroFacture, '')) LIKE LOWER(CONCAT(CONCAT('%', :search), '%'))))")
    Page<Vente> findByVendeurIdPaginatedWithSearch(@Param("vendeurId") Long vendeurId, @Param("search") String search, Pageable pageable);

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

    /** Batch : max dateVente par entreprise (dernière utilisation métier). Retourne [entrepriseId, maxDate]. */
    @Query("SELECT v.boutique.entreprise.id, MAX(v.dateVente) FROM Vente v WHERE v.boutique.entreprise.id IN :ids GROUP BY v.boutique.entreprise.id")
    List<Object[]> findMaxDateVenteByEntrepriseIdIn(@Param("ids") List<Long> ids);

    // Récupère les ventes récentes d'une entreprise (triées par date décroissante)
    @Query("SELECT v FROM Vente v WHERE v.boutique.entreprise.id = :entrepriseId ORDER BY v.dateVente DESC")
    List<Vente> findRecentVentesByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Récupère les ventes effectuées avec des caisses fermées
    @Query("SELECT v FROM Vente v WHERE v.boutique.entreprise.id = :entrepriseId AND v.caisse.statut = 'FERMEE' ORDER BY v.dateVente DESC")
    List<Vente> findByEntrepriseIdAndCaisseFermee(@Param("entrepriseId") Long entrepriseId);

    /** Ventes par liste de caisses (pour pagination comptabilité scalable). */
    List<Vente> findByCaisse_IdIn(List<Long> caisseIds);

    /** Pour pagination dettes POS : charger ventes par IDs avec relations (évite N+1). */
    @Query("SELECT DISTINCT v FROM Vente v " +
           "LEFT JOIN FETCH v.produits p " +
           "LEFT JOIN FETCH p.produit " +
           "LEFT JOIN FETCH v.vendeur " +
           "LEFT JOIN FETCH v.boutique " +
           "LEFT JOIN FETCH v.client " +
           "LEFT JOIN FETCH v.entrepriseClient " +
           "WHERE v.id IN :ids")
    List<Vente> findByIdInWithDetailsForDettes(@Param("ids") List<Long> ids);

    // Compter toutes les ventes des boutiques d'une entreprise
    @Query("SELECT COUNT(v) FROM Vente v WHERE v.boutique.entreprise.id = :entrepriseId")
    long countByEntrepriseId(@Param("entrepriseId") Long entrepriseId);

    // Ventes à crédit (dette clients) pour une entreprise
    List<Vente> findByBoutique_Entreprise_IdAndModePaiement(Long entrepriseId, ModePaiement modePaiement);

    // Statistiques globales combinées (COUNT et SUM brut en une seule requête)
    // Retourne une liste avec une seule ligne : [0] = totalVentes (Long), [1] = montantTotalBrut (Double)
    @Query("SELECT COUNT(v), COALESCE(SUM(v.montantTotal), 0) FROM Vente v " +
           "WHERE v.boutique.entreprise.id = :entrepriseId " +
           "AND v.dateVente >= :dateDebut AND v.dateVente < :dateFin")
    List<Object[]> getStatistiquesGlobalesByEntrepriseIdAndPeriode(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin);

    // Même statistiques avec filtres optionnels vendeur et boutique
    // Retourne: [0] = totalVentes, [1] = montantTotalBrut, [2] = montantTotalNet, [3] = totalRembourse (nb ventes avec remboursement)
    @Query(value = "SELECT COUNT(v.id), " +
           "COALESCE(SUM(v.montant_total), 0), " +
           "COALESCE(SUM(v.montant_total - COALESCE(v.montant_total_rembourse, 0)), 0), " +
           "COALESCE(SUM(CASE WHEN COALESCE(v.montant_total_rembourse, 0) > 0 THEN 1 ELSE 0 END), 0) " +
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

    // Top 3 vendeurs par entreprise et période (montant_total NET des remboursements)
    @Query(value = "SELECT v.vendeur_id, u.nom_complet, COUNT(v.id) as nombre_ventes, " +
           "COALESCE(SUM(v.montant_total - COALESCE(v.montant_total_rembourse, 0)), 0) as montant_total " +
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

    // TOUS les vendeurs par entreprise et période (triés par montant décroissant, montant NET des remboursements)
    @Query(value = "SELECT v.vendeur_id, u.nom_complet, COUNT(v.id) as nombre_ventes, " +
           "COALESCE(SUM(v.montant_total - COALESCE(v.montant_total_rembourse, 0)), 0) as montant_total " +
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

    // Même avec filtres optionnels vendeur et boutique (montant NET des remboursements)
    @Query(value = "SELECT v.vendeur_id, u.nom_complet, COUNT(v.id) as nombre_ventes, " +
           "COALESCE(SUM(v.montant_total - COALESCE(v.montant_total_rembourse, 0)), 0) as montant_total " +
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
    // Retourne: [0] = montantCaisseOuverteNet, [1] = montantCaisseFermeeNet (après remboursements)
    @Query(value = "SELECT " +
           "COALESCE(SUM(CASE WHEN c.statut = 'OUVERTE' THEN (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) ELSE 0 END), 0) as montant_ouverte, " +
           "COALESCE(SUM(CASE WHEN c.statut = 'FERMEE' THEN (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) ELSE 0 END), 0) as montant_fermee " +
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
           "COALESCE(SUM(CASE WHEN c.statut = 'OUVERTE' THEN (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) ELSE 0 END), 0) as montant_ouverte, " +
           "COALESCE(SUM(CASE WHEN c.statut = 'FERMEE' THEN (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) ELSE 0 END), 0) as montant_fermee " +
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
    // Retourne: [0] = totalVentes, [1] = montantTotalBrut, [2] = montantTotalNet, [3] = totalRembourse (nb ventes remboursées)
    @Query(value = "SELECT COUNT(v.id), " +
           "COALESCE(SUM(v.montant_total), 0), " +
           "COALESCE(SUM(v.montant_total - COALESCE(v.montant_total_rembourse, 0)), 0), " +
           "COALESCE(SUM(CASE WHEN COALESCE(v.montant_total_rembourse, 0) > 0 THEN 1 ELSE 0 END), 0) " +
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

    // Montants des ventes d'un vendeur par statut de caisse (OUVERTE et FERMEE), net des remboursements
    // Retourne: [0] = montantCaisseOuverte, [1] = montantCaisseFermee
    @Query(value = "SELECT " +
           "COALESCE(SUM(CASE WHEN c.statut = 'OUVERTE' THEN (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) ELSE 0 END), 0) as montant_ouverte, " +
           "COALESCE(SUM(CASE WHEN c.statut = 'FERMEE' THEN (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) ELSE 0 END), 0) as montant_fermee " +
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

    // ----- Dettes POS (ventes à crédit) : pagination côté base -----
    @Query(value = "SELECT COUNT(v.id) FROM vente v INNER JOIN caisse c ON v.caisse_id = c.id INNER JOIN boutique b ON v.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND v.mode_paiement = 'CREDIT' " +
            "AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0 " +
            "AND (:dateDebut IS NULL OR v.date_vente >= :dateDebut) AND (:dateFin IS NULL OR v.date_vente < :dateFin) " +
            "AND (:vendeurId IS NULL OR v.vendeur_id = :vendeurId) AND (:boutiqueId IS NULL OR v.boutique_id = :boutiqueId)", nativeQuery = true)
    long countDettesPos(
            @Param("entrepriseId") Long entrepriseId,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            @Param("vendeurId") Long vendeurId,
            @Param("boutiqueId") Long boutiqueId);

    @Query(value = "SELECT v.id FROM vente v INNER JOIN caisse c ON v.caisse_id = c.id INNER JOIN boutique b ON v.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND v.mode_paiement = 'CREDIT' " +
            "AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0 " +
            "AND (:dateDebut IS NULL OR v.date_vente >= :dateDebut) AND (:dateFin IS NULL OR v.date_vente < :dateFin) " +
            "AND (:vendeurId IS NULL OR v.vendeur_id = :vendeurId) AND (:boutiqueId IS NULL OR v.boutique_id = :boutiqueId) " +
            "ORDER BY v.date_vente ASC, v.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findDettesPosIdsOrderByDateAsc(
            @Param("entrepriseId") Long entrepriseId, @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin, @Param("vendeurId") Long vendeurId, @Param("boutiqueId") Long boutiqueId,
            @Param("limit") int limit, @Param("offset") int offset);
    @Query(value = "SELECT v.id FROM vente v INNER JOIN caisse c ON v.caisse_id = c.id INNER JOIN boutique b ON v.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND v.mode_paiement = 'CREDIT' " +
            "AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0 " +
            "AND (:dateDebut IS NULL OR v.date_vente >= :dateDebut) AND (:dateFin IS NULL OR v.date_vente < :dateFin) " +
            "AND (:vendeurId IS NULL OR v.vendeur_id = :vendeurId) AND (:boutiqueId IS NULL OR v.boutique_id = :boutiqueId) " +
            "ORDER BY v.date_vente DESC, v.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findDettesPosIdsOrderByDateDesc(
            @Param("entrepriseId") Long entrepriseId, @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin, @Param("vendeurId") Long vendeurId, @Param("boutiqueId") Long boutiqueId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = "SELECT v.id FROM vente v INNER JOIN caisse c ON v.caisse_id = c.id INNER JOIN boutique b ON v.boutique_id = b.id " +
            "LEFT JOIN user u ON v.vendeur_id = u.id " +
            "WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND v.mode_paiement = 'CREDIT' " +
            "AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0 " +
            "AND (:dateDebut IS NULL OR v.date_vente >= :dateDebut) AND (:dateFin IS NULL OR v.date_vente < :dateFin) " +
            "AND (:vendeurId IS NULL OR v.vendeur_id = :vendeurId) AND (:boutiqueId IS NULL OR v.boutique_id = :boutiqueId) " +
            "ORDER BY u.nom_complet ASC, v.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findDettesPosIdsOrderByVendeurAsc(
            @Param("entrepriseId") Long entrepriseId, @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin, @Param("vendeurId") Long vendeurId, @Param("boutiqueId") Long boutiqueId,
            @Param("limit") int limit, @Param("offset") int offset);
    @Query(value = "SELECT v.id FROM vente v INNER JOIN caisse c ON v.caisse_id = c.id INNER JOIN boutique b ON v.boutique_id = b.id " +
            "LEFT JOIN user u ON v.vendeur_id = u.id " +
            "WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND v.mode_paiement = 'CREDIT' " +
            "AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0 " +
            "AND (:dateDebut IS NULL OR v.date_vente >= :dateDebut) AND (:dateFin IS NULL OR v.date_vente < :dateFin) " +
            "AND (:vendeurId IS NULL OR v.vendeur_id = :vendeurId) AND (:boutiqueId IS NULL OR v.boutique_id = :boutiqueId) " +
            "ORDER BY u.nom_complet DESC, v.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findDettesPosIdsOrderByVendeurDesc(
            @Param("entrepriseId") Long entrepriseId, @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin, @Param("vendeurId") Long vendeurId, @Param("boutiqueId") Long boutiqueId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = "SELECT v.id FROM vente v INNER JOIN caisse c ON v.caisse_id = c.id INNER JOIN boutique b ON v.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND v.mode_paiement = 'CREDIT' " +
            "AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0 " +
            "AND (:dateDebut IS NULL OR v.date_vente >= :dateDebut) AND (:dateFin IS NULL OR v.date_vente < :dateFin) " +
            "AND (:vendeurId IS NULL OR v.vendeur_id = :vendeurId) AND (:boutiqueId IS NULL OR v.boutique_id = :boutiqueId) " +
            "ORDER BY b.nom_boutique ASC, v.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findDettesPosIdsOrderByBoutiqueAsc(
            @Param("entrepriseId") Long entrepriseId, @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin, @Param("vendeurId") Long vendeurId, @Param("boutiqueId") Long boutiqueId,
            @Param("limit") int limit, @Param("offset") int offset);
    @Query(value = "SELECT v.id FROM vente v INNER JOIN caisse c ON v.caisse_id = c.id INNER JOIN boutique b ON v.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND v.mode_paiement = 'CREDIT' " +
            "AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0 " +
            "AND (:dateDebut IS NULL OR v.date_vente >= :dateDebut) AND (:dateFin IS NULL OR v.date_vente < :dateFin) " +
            "AND (:vendeurId IS NULL OR v.vendeur_id = :vendeurId) AND (:boutiqueId IS NULL OR v.boutique_id = :boutiqueId) " +
            "ORDER BY b.nom_boutique DESC, v.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findDettesPosIdsOrderByBoutiqueDesc(
            @Param("entrepriseId") Long entrepriseId, @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin, @Param("vendeurId") Long vendeurId, @Param("boutiqueId") Long boutiqueId,
            @Param("limit") int limit, @Param("offset") int offset);

    @Query(value = "SELECT v.id FROM vente v INNER JOIN caisse c ON v.caisse_id = c.id INNER JOIN boutique b ON v.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND v.mode_paiement = 'CREDIT' " +
            "AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0 " +
            "AND (:dateDebut IS NULL OR v.date_vente >= :dateDebut) AND (:dateFin IS NULL OR v.date_vente < :dateFin) " +
            "AND (:vendeurId IS NULL OR v.vendeur_id = :vendeurId) AND (:boutiqueId IS NULL OR v.boutique_id = :boutiqueId) " +
            "ORDER BY (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) ASC, v.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findDettesPosIdsOrderByMontantAsc(
            @Param("entrepriseId") Long entrepriseId, @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin, @Param("vendeurId") Long vendeurId, @Param("boutiqueId") Long boutiqueId,
            @Param("limit") int limit, @Param("offset") int offset);
    @Query(value = "SELECT v.id FROM vente v INNER JOIN caisse c ON v.caisse_id = c.id INNER JOIN boutique b ON v.boutique_id = b.id " +
            "WHERE b.entreprise_id = :entrepriseId AND c.statut = 'FERMEE' AND v.mode_paiement = 'CREDIT' " +
            "AND (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) > 0 " +
            "AND (:dateDebut IS NULL OR v.date_vente >= :dateDebut) AND (:dateFin IS NULL OR v.date_vente < :dateFin) " +
            "AND (:vendeurId IS NULL OR v.vendeur_id = :vendeurId) AND (:boutiqueId IS NULL OR v.boutique_id = :boutiqueId) " +
            "ORDER BY (v.montant_total - COALESCE(v.montant_total_rembourse, 0)) DESC, v.id DESC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<Long> findDettesPosIdsOrderByMontantDesc(
            @Param("entrepriseId") Long entrepriseId, @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin, @Param("vendeurId") Long vendeurId, @Param("boutiqueId") Long boutiqueId,
            @Param("limit") int limit, @Param("offset") int offset);

}