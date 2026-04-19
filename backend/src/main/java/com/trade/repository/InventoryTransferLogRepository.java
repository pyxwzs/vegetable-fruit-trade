package com.trade.repository;

import com.trade.entity.InventoryTransferLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryTransferLogRepository extends JpaRepository<InventoryTransferLog, Long> {
}
