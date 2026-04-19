package com.trade.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class SalesAnalysisDTO {

    private Summary summary;

    private List<TrendData> trend;

    private List<ProductRank> topProducts;

    private List<CustomerRank> topCustomers;

    private Map<String, Object> categoryDistribution;

    @Data
    public static class Summary {
        private BigDecimal totalSales;
        private BigDecimal totalProfit;
        private Integer orderCount;
        private Integer customerCount;
        private BigDecimal avgOrderValue;
        private BigDecimal profitMargin;
        private BigDecimal growthRate;
        private BigDecimal targetCompletion;
    }

    @Data
    public static class TrendData {
        private String period;
        private BigDecimal sales;
        private BigDecimal profit;
        private Integer orderCount;
    }

    @Data
    public static class ProductRank {
        private Long productId;
        private String productName;
        private String category;
        private Integer salesCount;
        private BigDecimal salesAmount;
        private BigDecimal profit;
        private BigDecimal profitMargin;
    }

    @Data
    public static class CustomerRank {
        private Long customerId;
        private String customerName;
        private Integer orderCount;
        private BigDecimal purchaseAmount;
        private BigDecimal profit;
        private String creditLevel;
    }
}
