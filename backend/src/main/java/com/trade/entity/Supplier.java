package com.trade.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "suppliers", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "supplier_code"}))
public class Supplier extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_code", nullable = false, length = 50)
    private String supplierCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String contact;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String address;

    @Enumerated(EnumType.STRING)
    private SupplierStatus status = SupplierStatus.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum SupplierStatus {
        ACTIVE, INACTIVE
    }
}
