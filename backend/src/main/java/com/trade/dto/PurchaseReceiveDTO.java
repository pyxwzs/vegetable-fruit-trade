package com.trade.dto;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseReceiveDTO {

    @NotNull(message = "采购订单ID不能为空")
    private Long purchaseOrderId;

    @NotNull(message = "收货日期不能为空")
    private LocalDate receiveDate;

    private Long warehouseId;

    @Valid
    private List<ReceiveItem> items;

    private String remark;

    @Data
    public static class ReceiveItem {

        @NotNull(message = "采购项ID不能为空")
        private Long orderItemId;

        private Long productId;

        @NotNull(message = "收货数量不能为空")
        private BigDecimal receiveQuantity;

        private String batchNo;

        private LocalDate productionDate;

        private LocalDate expiryDate;

        private String location;
    }
}