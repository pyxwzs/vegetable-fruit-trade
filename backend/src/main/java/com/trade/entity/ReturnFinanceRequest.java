package com.trade.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "return_finance_requests")
public class ReturnFinanceRequest extends TenantAwareEntity {

    public enum Kind {
        PURCHASE, SALES
    }

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kind kind;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_no", length = 50)
    private String orderNo;

    /** JSON 数组：[{productId, productName, quantity, price}, ...] */
    @Column(name = "lines_json", nullable = false, columnDefinition = "TEXT")
    private String linesJson;

    @Column(name = "return_amount", precision = 10, scale = 2)
    private BigDecimal returnAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(length = 500)
    private String rejectReason;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
