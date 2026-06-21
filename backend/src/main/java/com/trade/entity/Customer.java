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
@Table(name = "customers", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "customer_code"}))
public class Customer extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_code", nullable = false, length = 50)
    private String customerCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String contact;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String address;

    @Enumerated(EnumType.STRING)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum CustomerStatus {
        ACTIVE, INACTIVE
    }
}
