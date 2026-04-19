package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsKpiDTO {
    private BigDecimal totalSales;
    /** 相对上一同长区间的变化百分比，可为负 */
    private BigDecimal salesTrendPercent;

    private BigDecimal grossProfit;
    private BigDecimal profitTrendPercent;

    private long orderCount;
    private BigDecimal orderTrendPercent;

    private long tradingCustomerCount;
    private BigDecimal customerTrendPercent;
}
