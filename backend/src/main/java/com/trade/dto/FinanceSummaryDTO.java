package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 财务汇总数据 */
@Data
public class FinanceSummaryDTO {

    // ── 采购资金 ──
    private BigDecimal totalPurchaseAmount;   // 采购总额
    private BigDecimal totalPaidAmount;        // 已付金额
    private BigDecimal totalUnpaidAmount;      // 未付金额（应付账款）

    // ── 销售资金 ──
    private BigDecimal totalSalesAmount;       // 销售总额
    private BigDecimal totalReceivedAmount;    // 已收金额
    private BigDecimal totalUnreceivedAmount;  // 未收金额（应收账款）

    // ── 利润 ──
    private BigDecimal grossProfit;            // 毛利 = 已收 - 已付
    private BigDecimal grossMarginPercent;     // 毛利率（%）

    // ── 仓库资产 ──
    private BigDecimal totalWarehouseAsset;    // 全部仓库库存资产总价值
    private List<WarehouseAssetRow> warehouseAssets; // 各仓库资产

    // ── 近6个月趋势 ──
    private List<MonthRow> purchaseByMonth;
    private List<MonthRow> salesByMonth;

    @Data
    public static class MonthRow {
        private String month;
        private BigDecimal total;
        private BigDecimal settled;  // 已付 / 已收

        public MonthRow(String month, BigDecimal total, BigDecimal settled) {
            this.month = month;
            this.total = total;
            this.settled = settled;
        }
    }

    @Data
    public static class WarehouseAssetRow {
        private Long warehouseId;
        private String warehouseName;
        private BigDecimal assetValue;  // 数量 × 采购价

        public WarehouseAssetRow(Long warehouseId, String warehouseName, BigDecimal assetValue) {
            this.warehouseId = warehouseId;
            this.warehouseName = warehouseName;
            this.assetValue = assetValue;
        }
    }
}
