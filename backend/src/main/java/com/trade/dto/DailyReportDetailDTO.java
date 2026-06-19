package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DailyReportDetailDTO {
    private Long entityId;
    private String entityName;
    private int year;
    private int month;
    private List<DailyStatRowDTO> rows;
    private BigDecimal monthTotalAmount;
    private BigDecimal monthSettledAmount;
    private BigDecimal monthPendingAmount;
}
