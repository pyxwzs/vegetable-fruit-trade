package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class PurchaseOrderItemDTO {
    @NotNull(message = "商品不能为空")
    private Long productId;

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须为正数")
    private BigDecimal quantity;

    @NotNull(message = "单价不能为空")
    @Positive(message = "单价必须为正数")
    private BigDecimal price;

    private String remark;
}