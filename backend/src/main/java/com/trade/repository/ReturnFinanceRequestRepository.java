package com.trade.repository;

import com.trade.entity.ReturnFinanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface ReturnFinanceRequestRepository
        extends JpaRepository<ReturnFinanceRequest, Long>,
                JpaSpecificationExecutor<ReturnFinanceRequest> {

    /** 检查指定订单是否已有"进行中"的退货申请（PENDING 或 WH_APPROVED） */
    boolean existsByOrderIdAndKindAndStatusIn(
            Long orderId,
            ReturnFinanceRequest.Kind kind,
            List<ReturnFinanceRequest.Status> statuses);
}
