package com.trade.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sale_payments")
public class SalePayment extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private SalesOrder order;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private String paymentMethod;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
