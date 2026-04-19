package com.trade.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductStockDTO {

    private Long productId;

    private String productCode;

    private String productName;

    private Long warehouseId;

    private String warehouseName;

    private BigDecimal totalQuantity;

    private BigDecimal availableQuantity;

    private BigDecimal frozenQuantity;

    private Integer batchCount;

    private BigDecimal averagePrice;
}
