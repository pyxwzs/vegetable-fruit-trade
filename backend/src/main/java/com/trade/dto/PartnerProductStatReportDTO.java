package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PartnerProductStatReportDTO {
    private int year;
    private Integer month;
    private BigDecimal grandTotalAmount = BigDecimal.ZERO;
    private BigDecimal grandTotalQuantity = BigDecimal.ZERO;
    private List<PartnerProductGroupDTO> groups = new ArrayList<>();
}
