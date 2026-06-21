package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class SupplierMetricCompletionReportDTO {
    private String periodType;
    private String periodLabel;
    private Integer year;
    private Integer month;
    private BigDecimal totalTarget = BigDecimal.ZERO;
    private BigDecimal totalActual = BigDecimal.ZERO;
    private BigDecimal totalGap = BigDecimal.ZERO;
    private BigDecimal totalExcess = BigDecimal.ZERO;
    private BigDecimal totalShortfall = BigDecimal.ZERO;
    private Integer completionPercent = 0;
    private List<SupplierMetricCompletionGroupDTO> groups = new ArrayList<>();
}
