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
@Table(name = "inventory_transfer_logs")
public class InventoryTransferLog extends TenantAwareEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_inventory_id", nullable = false)
    private Long sourceInventoryId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "from_warehouse_id")
    private Warehouse fromWarehouse;

    @ManyToOne(optional = false)
    @JoinColumn(name = "to_warehouse_id")
    private Warehouse toWarehouse;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "batch_no", length = 50)
    private String batchNo;

    @Column(nullable = false, precision = 10, scale = 3)
    private BigDecimal quantity;

    @Column(name = "operator_username", length = 64)
    private String operatorUsername;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
