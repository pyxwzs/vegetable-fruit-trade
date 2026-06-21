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
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "product_code"}))
public class Product extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, length = 50)
    private String productCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String category;

    @Column(length = 20)
    private String unit;

    private String specification;

    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.ENABLED;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum ProductStatus {
        ENABLED, DISABLED
    }
}
