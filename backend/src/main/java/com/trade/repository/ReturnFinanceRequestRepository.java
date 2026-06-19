package com.trade.repository;

import com.trade.entity.ReturnFinanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReturnFinanceRequestRepository
        extends JpaRepository<ReturnFinanceRequest, Long>,
                JpaSpecificationExecutor<ReturnFinanceRequest> {
}
