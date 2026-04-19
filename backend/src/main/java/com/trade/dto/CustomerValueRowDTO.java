package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerValueRowDTO {
    private int rank;
    private Long customerId;
    private String customerName;
    private String creditLevel;
    private long orderCount;
    private BigDecimal revenue;
    private BigDecimal estimatedCost;
    private BigDecimal grossProfit;
    /** 0~1 */
    private BigDecimal profitMargin;
}
