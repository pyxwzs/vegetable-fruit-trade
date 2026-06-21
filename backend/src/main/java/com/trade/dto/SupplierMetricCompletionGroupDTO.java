package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class SupplierMetricCompletionGroupDTO {
    private Long supplierId;
    private String supplierName;
    private BigDecimal totalTarget = BigDecimal.ZERO;
    private BigDecimal totalActual = BigDecimal.ZERO;
    /** 差额 = 实际 - 指标（正为超量，负为缺量） */
    private BigDecimal totalGap = BigDecimal.ZERO;
    private BigDecimal totalExcess = BigDecimal.ZERO;
    private BigDecimal totalShortfall = BigDecimal.ZERO;
    private Integer completionPercent = 0;
    private List<SupplierProductMetricDTO> items = new ArrayList<>();
}
