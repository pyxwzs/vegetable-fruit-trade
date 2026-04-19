package com.trade.repository;

import com.trade.entity.InventoryStocktakeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryStocktakeLogRepository extends JpaRepository<InventoryStocktakeLog, Long> {
}
