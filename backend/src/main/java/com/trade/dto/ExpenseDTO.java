package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExpenseDTO {
    private Long id;
    private LocalDate expenseDate;
    private String category;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
