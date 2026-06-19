package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MonthlyOverviewDTO {
    private int year;
    private int month;

    // 核心指标
    private BigDecimal purchaseTotal;
    private BigDecimal salesTotal;
    private BigDecimal grossProfit;
    private BigDecimal paidToFarmers;
    private BigDecimal unpaidToFarmers;
    private BigDecimal collectedFromCustomers;
    private BigDecimal uncollectedFromCustomers;
    private BigDecimal cashDifference;
    // 其他支出
    private BigDecimal otherExpenses;
    private BigDecimal netProfit;

    // 排行
    private List<BalanceRowDTO> farmerUnpaidRanking;
    private List<BalanceRowDTO> customerUnreceivedRanking;
    private List<ProductGrossDTO> productGrossRanking;

    @Data
    public static class BalanceRowDTO {
        private String name;
        private BigDecimal total;
        private BigDecimal settled;
        private BigDecimal unpaid;
    }

    @Data
    public static class ProductGrossDTO {
        private String productName;
        private String unit;
        private BigDecimal purchaseAmount;
        private BigDecimal salesAmount;
        private BigDecimal grossProfit;
        private String grossMargin;
    }
}
