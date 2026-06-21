package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PartnerTradeSectionDTO {
    private Long entityId;
    private String entityName;
    private BigDecimal totalQuantity = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private List<PartnerProductLineDTO> items = new ArrayList<>();
}
