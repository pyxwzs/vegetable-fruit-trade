package com.trade.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventory_transfer_logs")
public class InventoryTransferLog {
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

    @Column(length = 500)
    private String remark;

    @Column(name = "operator_username", length = 64)
    private String operatorUsername;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
