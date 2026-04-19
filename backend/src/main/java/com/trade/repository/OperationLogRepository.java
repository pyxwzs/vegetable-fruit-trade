package com.trade.repository;

import com.trade.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long>, JpaSpecificationExecutor<OperationLog> {

    @Modifying
    @Query("DELETE FROM OperationLog o WHERE o.createTime < :beforeDate")
    void deleteByCreateTimeBefore(@Param("beforeDate") LocalDateTime beforeDate);
}
