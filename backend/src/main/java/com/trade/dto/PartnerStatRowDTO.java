package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PartnerStatRowDTO {
    private Long entityId;
    private String name;
    private long orderCount;
    private BigDecimal totalAmount;
    private BigDecimal settledAmount;
    private BigDecimal pendingAmount;
}
