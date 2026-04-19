package com.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportDTO {
    private LocalDate date;
    /** 当日已出库/完成订单的销售额（按订单业务日） */
    private BigDecimal realizedSales;
    /** 当日新单数（非取消） */
    private long newOrderCount;
    /** 当日新采购单数（非取消） */
    private long newPurchaseOrderCount;
    private long pendingPurchaseCount;
    private long pendingSalesCount;
    private int lowStockLineCount;
    private int expiringSkuCount;
    private int creditWarningCount;
}
