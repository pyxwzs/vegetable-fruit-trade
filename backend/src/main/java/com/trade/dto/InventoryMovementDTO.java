package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
public class InventoryMovementDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 入库时可空，系统将使用已有仓库或自动创建默认仓库 */
    private Long warehouseId;

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须为正数")
    private BigDecimal quantity;
}
