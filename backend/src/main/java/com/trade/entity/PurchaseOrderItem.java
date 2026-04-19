package com.trade.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    @JsonIgnore
    private PurchaseOrder purchaseOrder;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(precision = 10, scale = 3)
    private BigDecimal quantity; // 数量

    @Column(precision = 10, scale = 3)
    private BigDecimal returnedQuantity = BigDecimal.ZERO; // 累计已退货数量

    @Column(precision = 10, scale = 2)
    private BigDecimal price; // 单价

    @Column(precision = 10, scale = 2)
    private BigDecimal amount; // 金额 = 数量 * 单价

    private String remark;
}