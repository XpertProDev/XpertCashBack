package com.xpertcash.repository;

import java.util.List;

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

}
