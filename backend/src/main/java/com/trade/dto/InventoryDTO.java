package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InventoryDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private BigDecimal quantity;
    private String remark;
}
