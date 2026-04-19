package com.trade.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "warehouses")
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 200)
    private String address;

    private String manager;

    private String phone;

    private Double area; // 面积

    @Enumerated(EnumType.STRING)
    private WarehouseType type = WarehouseType.NORMAL;

    @Enumerated(EnumType.STRING)
    private WarehouseStatus status = WarehouseStatus.ACTIVE;

    public enum WarehouseType {
        NORMAL, COLD, FREEZE
    }

    public enum WarehouseStatus {
        ACTIVE, INACTIVE, MAINTENANCE
    }
}