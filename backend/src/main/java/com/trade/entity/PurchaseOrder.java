package com.trade.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String orderNo; // 订单号

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "purchaser_id")
    @JsonIgnoreProperties({"roles", "password", "mfaLoginEnabled", "loginAlertEmailEnabled"})
    private User purchaser; // 采购员

    private LocalDate orderDate; // 下单日期

    private LocalDate expectedDeliveryDate; // 预计交货日期

    private LocalDate deliveryDate; // 实际交货日期

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO; // 总金额

    @Column(precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO; // 已付金额

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO; // 折扣金额

    @Column(length = 20)
    private String paymentMethod; // 支付方式

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID; // 支付状态

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING; // 订单状态

    private String remark;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum OrderStatus {
        PENDING, APPROVED, SHIPPED, RECEIVED, COMPLETED, CANCELLED
    }

    public enum PaymentStatus {
        UNPAID, PARTIAL, PAID
    }
}