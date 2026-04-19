package com.trade.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ProfitAnalysisDTO {

    private ProfitSummary summary;

    private List<ProfitTrend> trend;

    private List<ProfitMarginRank> productMargin;

    private Map<String, BigDecimal> costBreakdown;

    @Data
    public static class ProfitSummary {
        private BigDecimal totalRevenue;
        private BigDecimal totalCost;
        private BigDecimal grossProfit;
        private BigDecimal netProfit;
        private BigDecimal grossMargin;
        private BigDecimal netMargin;
        private BigDecimal operatingExpenses;
        private BigDecimal taxExpenses;
        private BigDecimal yoyGrowth;
        private BigDecimal momGrowth;
    }

    @Data
    public static class ProfitTrend {
        private String period;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
        private BigDecimal margin;
    }

    @Data
    public static class ProfitMarginRank {
        private Long productId;
        private String productName;
        private BigDecimal revenue;
        private BigDecimal cost;
        private BigDecimal profit;
        private BigDecimal margin;
        private String category;
    }
}