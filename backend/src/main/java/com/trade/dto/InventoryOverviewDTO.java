package com.trade.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class InventoryOverviewDTO {
    private long skuCount;
    private BigDecimal totalValue = BigDecimal.ZERO;
    private List<Item> items = new ArrayList<>();

    @Data
    public static class Item {
        private Long productId;
        private String productName;
        private String category;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal unitCost;
        private BigDecimal value;
    }
}
