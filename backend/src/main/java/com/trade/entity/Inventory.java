package com.trade.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "inventories")
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(length = 50)
    private String batchNo; // 批次号

    @Column(precision = 10, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO; // 当前库存数量

    @Column(precision = 10, scale = 3)
    private BigDecimal availableQuantity = BigDecimal.ZERO; // 可用数量

    @Column(precision = 10, scale = 3)
    private BigDecimal frozenQuantity = BigDecimal.ZERO; // 冻结数量（已锁定）

    private LocalDate productionDate; // 生产日期

    private LocalDate expiryDate; // 过期日期

    @Column(precision = 10, scale = 2)
    private BigDecimal purchasePrice; // 采购价

    private String location; // 库位

    @Enumerated(EnumType.STRING)
    private InventoryStatus status = InventoryStatus.NORMAL;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum InventoryStatus {
        NORMAL, EXPIRING, EXPIRED, FROZEN
    }
}