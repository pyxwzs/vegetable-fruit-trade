package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MonthlyReportDTO {
    private Long entityId;
    private String entityName;
    private int year;
    private List<MonthlyStatRowDTO> rows;
    private BigDecimal yearTotalAmount;
    private BigDecimal yearSettledAmount;
    private BigDecimal yearPendingAmount;
}
