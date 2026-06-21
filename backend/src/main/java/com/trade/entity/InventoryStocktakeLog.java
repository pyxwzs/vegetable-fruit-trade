package com.trade.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "inventory_stocktake_logs")
public class InventoryStocktakeLog extends TenantAwareEntity {
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

    @Column(name = "operator_username", length = 64)
    private String operatorUsername;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
