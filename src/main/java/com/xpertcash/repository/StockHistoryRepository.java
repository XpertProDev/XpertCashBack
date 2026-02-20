package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Stock;
import com.xpertcash.entity.StockHistory;

import jakarta.transaction.Transactional;

@Repository
public interface StockHistoryRepository  extends JpaRepository<StockHistory, Long> {
     List<StockHistory> findByStock(Stock stock);

     @Modifying
    @Transactional
    @Query("DELETE FROM StockHistory s WHERE s.stock = :stock")
    void deleteByStock(@Param("stock") Stock stock);

    /** Pagination côté base : historique de stock par entreprise (isolation multi-tenant). */
    @Query(value = "SELECT DISTINCT sh FROM StockHistory sh LEFT JOIN FETCH sh.user " +
            "JOIN sh.stock s JOIN s.boutique b JOIN b.entreprise e WHERE e.id = :entrepriseId",
            countQuery = "SELECT COUNT(DISTINCT sh) FROM StockHistory sh JOIN sh.stock s JOIN s.boutique b JOIN b.entreprise e WHERE e.id = :entrepriseId")
    Page<StockHistory> findByStock_Boutique_Entreprise_Id(@Param("entrepriseId") Long entrepriseId, Pageable pageable);

}
