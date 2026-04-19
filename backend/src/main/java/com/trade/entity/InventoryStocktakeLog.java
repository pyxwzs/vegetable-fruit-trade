package com.trade.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_stocktake_logs")
public class InventoryStocktakeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inventory_id")
    private Inventory inventory;

    @Column(name = "qty_before", nullable = false, precision = 10, scale = 3)
    private BigDecimal qtyBefore;

    @Column(name = "qty_after", nullable = false, precision = 10, scale = 3)
    private BigDecimal qtyAfter;

    @Column(name = "diff_qty", nullable = false, precision = 10, scale = 3)
    private BigDecimal diffQty;

    @Column(length = 500)
    private String remark;

    @Column(name = "operator_username", length = 64)
    private String operatorUsername;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
