package com.trade.repository;

import com.trade.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("SELECT e FROM Expense e WHERE " +
           "(:keyword IS NULL OR e.category LIKE %:keyword%) " +
           "AND (:year IS NULL OR YEAR(e.expenseDate) = :year) " +
           "AND (:month IS NULL OR MONTH(e.expenseDate) = :month) " +
           "AND (:startDate IS NULL OR e.expenseDate >= :startDate) " +
           "AND (:endDate IS NULL OR e.expenseDate <= :endDate) " +
           "ORDER BY e.expenseDate DESC, e.id DESC")
    Page<Expense> search(@Param("keyword") String keyword,
                         @Param("year") Integer year,
                         @Param("month") Integer month,
                         @Param("startDate") LocalDate startDate,
                         @Param("endDate") LocalDate endDate,
                         Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE YEAR(e.expenseDate) = :year")
    BigDecimal sumByYear(@Param("year") int year);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE YEAR(e.expenseDate) = :year AND MONTH(e.expenseDate) = :month")
    BigDecimal sumByMonth(@Param("year") int year, @Param("month") int month);
}
