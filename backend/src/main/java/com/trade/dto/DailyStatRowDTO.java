package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class DailyStatRowDTO {
    private LocalDate date;
    private long orderCount;
    private BigDecimal totalAmount;
    private BigDecimal settledAmount;
    private BigDecimal pendingAmount;
}
