package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class InventoryMovementDTO {
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "仓库ID不能为空")
    private Long warehouseId;

    private String batchNo;

    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须为正数")
    private BigDecimal quantity;

    private BigDecimal price;
    private LocalDate productionDate;
    /** 可选手动指定过期日（调拨入库等场景优先于按保质期推算） */
    private LocalDate expiryDate;
    private String location;
    private String remark;
}
