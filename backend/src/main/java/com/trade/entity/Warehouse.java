package com.trade.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "warehouses", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "code"}))
public class Warehouse extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    private WarehouseStatus status = WarehouseStatus.ACTIVE;

    public enum WarehouseStatus {
        ACTIVE, INACTIVE
    }
}
