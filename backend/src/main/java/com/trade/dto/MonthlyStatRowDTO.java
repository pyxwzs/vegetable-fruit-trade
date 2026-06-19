package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MonthlyStatRowDTO {
    private int month;
    private long orderCount;
    private BigDecimal totalAmount;
    private BigDecimal settledAmount;
    private BigDecimal pendingAmount;
}
