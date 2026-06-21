package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class TradeStatisticsReportDTO {
    private int year;
    /** null 表示全年 */
    private Integer month;
    private BigDecimal supplierGrandTotal = BigDecimal.ZERO;
    private BigDecimal customerGrandTotal = BigDecimal.ZERO;
    private List<PartnerTradeSectionDTO> suppliers = new ArrayList<>();
    private List<PartnerTradeSectionDTO> customers = new ArrayList<>();
    private List<ProductTradeStatDTO> products = new ArrayList<>();
}
