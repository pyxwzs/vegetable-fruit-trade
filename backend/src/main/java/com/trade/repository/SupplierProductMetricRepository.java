package com.trade.repository;

import com.trade.entity.SupplierProductMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierProductMetricRepository extends JpaRepository<SupplierProductMetric, Long>,
        JpaSpecificationExecutor<SupplierProductMetric> {

    boolean existsBySupplier_IdAndProduct_IdAndPeriodTypeAndYearAndMonth(
            Long supplierId, Long productId, SupplierProductMetric.PeriodType periodType,
            Integer year, Integer month);

    boolean existsBySupplier_IdAndProduct_IdAndPeriodTypeAndYearAndMonthAndIdNot(
            Long supplierId, Long productId, SupplierProductMetric.PeriodType periodType,
            Integer year, Integer month, Long id);

    List<SupplierProductMetric> findByPeriodTypeAndYearAndMonthAndStatus(
            SupplierProductMetric.PeriodType periodType, Integer year, Integer month,
            SupplierProductMetric.MetricStatus status);
}
