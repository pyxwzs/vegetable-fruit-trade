package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InventoryDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private String batchNo;
    private BigDecimal quantity;
    private BigDecimal availableQuantity;
    private BigDecimal frozenQuantity;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private BigDecimal purchasePrice;
    private String location;
    private String status;
}
