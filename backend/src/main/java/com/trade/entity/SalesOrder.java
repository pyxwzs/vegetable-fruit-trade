package com.trade.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sales_orders", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "order_no"}))
public class SalesOrder extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private LocalDate orderDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal receivedAmount = BigDecimal.ZERO;

    @Column(length = 20)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SalesOrderItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum OrderStatus {
        PENDING, COMPLETED, CANCELLED
    }

    public enum PaymentStatus {
        UNPAID, PARTIAL, PAID
    }
}
