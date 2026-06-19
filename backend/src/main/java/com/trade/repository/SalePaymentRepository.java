package com.trade.repository;

import com.trade.entity.SalePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SalePaymentRepository extends JpaRepository<SalePayment, Long> {

    List<SalePayment> findByOrderIdOrderByPaymentDateDesc(Long orderId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM SalePayment p WHERE p.order.id = :orderId")
    BigDecimal sumByOrderId(@Param("orderId") Long orderId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM SalePayment p " +
           "WHERE YEAR(p.paymentDate) = :year AND MONTH(p.paymentDate) = :month")
    BigDecimal sumByMonth(@Param("year") int year, @Param("month") int month);
}
