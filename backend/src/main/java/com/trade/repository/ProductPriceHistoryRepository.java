package com.trade.repository;

import com.trade.entity.ProductPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductPriceHistoryRepository extends JpaRepository<ProductPriceHistory, Long> {
    List<ProductPriceHistory> findByProduct_IdOrderByCreatedAtDesc(Long productId);

    void deleteByProduct_Id(Long productId);
}
