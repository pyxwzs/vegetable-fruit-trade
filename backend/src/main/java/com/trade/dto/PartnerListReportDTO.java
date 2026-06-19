package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PartnerListReportDTO {
    private int year;
    private Integer month;
    private List<PartnerStatRowDTO> rows;
    private BigDecimal totalAmount;
    private BigDecimal settledAmount;
    private BigDecimal pendingAmount;
}
