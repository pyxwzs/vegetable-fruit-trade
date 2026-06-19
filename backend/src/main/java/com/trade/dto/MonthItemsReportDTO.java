package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MonthItemsReportDTO {
    private Long entityId;
    private String entityName;
    private int year;
    private int month;
    private List<OrderItemDetailDTO> items;
    private BigDecimal totalAmount;
    private BigDecimal totalQuantity;
}
