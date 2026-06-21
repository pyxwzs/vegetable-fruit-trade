package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PartnerProductGroupDTO {
    private Long entityId;
    private String entityName;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal totalQuantity = BigDecimal.ZERO;
    private List<PartnerProductRowDTO> products = new ArrayList<>();
}
