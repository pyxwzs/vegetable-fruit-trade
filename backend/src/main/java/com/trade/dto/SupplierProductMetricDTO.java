package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SupplierProductMetricDTO {

    private Long id;

    @NotNull(message = "请选择供应商")
    private Long supplierId;

    private String supplierName;

    @NotNull(message = "请选择商品")
    private Long productId;

    private String productName;

    private String productUnit;

    /** YEAR 年指标 / MONTH 月指标 */
    @NotNull(message = "请选择指标类型")
    private String periodType;

    private String periodLabel;

    @NotNull(message = "请填写年份")
    private Integer year;

    /** 月指标 1-12；年指标为 0 */
    private Integer month;

    @NotNull(message = "请填写订量指标")
    private BigDecimal targetQty;

    /** 实际采购量（只读，来自采购单） */
    private BigDecimal actualQty;

    /** 差额 = 实际 - 指标（正超量，负缺量） */
    private BigDecimal gapQty;

    /** 超量 = max(0, 差额) */
    private BigDecimal excessQty;

    /** 缺量 = max(0, 指标 - 实际) */
    private BigDecimal shortfallQty;

    private Integer completionPercent;

    @Size(max = 500)
    private String remark;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
