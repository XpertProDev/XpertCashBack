package com.xpertcash.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xpertcash.entity.Stock;
import com.xpertcash.entity.StockHistory;

@Repository
public interface StockHistoryRepository  extends JpaRepository<StockHistory, Long> {
     List<StockHistory> findByStock(Stock stock);

}
