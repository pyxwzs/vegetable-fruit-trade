package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfitSummaryDTO {
    private BigDecimal revenue;
    /** 按商品采购参考价估算的成本 */
    private BigDecimal estimatedCost;
    private BigDecimal grossProfit;
    /** 毛利率 0~1 */
    private BigDecimal grossMargin;
    private List<CategorySalesDTO> salesByCategory = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySalesDTO {
        private String categoryName;
        private BigDecimal amount;
    }
}
