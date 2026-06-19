package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class InventoryMovementDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须为正数")
    private BigDecimal quantity;
}
