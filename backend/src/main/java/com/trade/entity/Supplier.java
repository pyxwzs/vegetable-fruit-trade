package com.trade.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String supplierCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String contact; // 联系人

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(length = 200)
    private String address;

    private String taxNumber; // 税号

    private String bankName; // 开户行

    private String bankAccount; // 银行账号

    @Column(precision = 5, scale = 2)
    private BigDecimal creditRating; // 信用评级 0~5

    private Integer deliveryOnTimeRate; // 准时交货率

    @Column(precision = 5, scale = 2)
    private BigDecimal qualityPassRate; // 质量合格率

    private String remark;

    @Enumerated(EnumType.STRING)
    private SupplierStatus status = SupplierStatus.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum SupplierStatus {
        ACTIVE, INACTIVE, BLACKLISTED
    }
}