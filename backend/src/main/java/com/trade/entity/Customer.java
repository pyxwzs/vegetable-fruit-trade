package com.trade.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String customerCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String contact;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 200)
    private String address;

    private String taxNumber;

    @Enumerated(EnumType.STRING)
    private CustomerType type = CustomerType.RETAIL; // 客户类型

    @Enumerated(EnumType.STRING)
    private CreditLevel creditLevel = CreditLevel.B; // 信用等级

    @Column(precision = 10, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO; // 信用额度

    @Column(precision = 10, scale = 2)
    private BigDecimal totalPurchaseAmount = BigDecimal.ZERO; // 累计采购额

    private Integer purchaseCount = 0; // 采购次数

    private String remark;

    @Enumerated(EnumType.STRING)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum CustomerType {
        WHOLESALE, RETAIL, CHAIN, OTHER
    }

    public enum CreditLevel {
        A, B, C, D
    }

    public enum CustomerStatus {
        ACTIVE, INACTIVE, FROZEN
    }
}