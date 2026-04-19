package com.trade.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退货申请两阶段流程：
 * 采购员/销售员提交 → 仓管员审批（库存变动）→ 财务审批（金额变动）。
 */
@Data
@Entity
@Table(name = "return_finance_requests")
public class ReturnFinanceRequest {

    public enum Kind {
        PURCHASE, SALES
    }

    public enum Status {
        /** 已提交，等待仓库审批 */
        PENDING,
        /** 仓库已审批：库存已变动，等待财务审批 */
        WH_APPROVED,
        /** 仓库已拒绝：库存不变动，流程终止 */
        WH_REJECTED,
        /** 财务已审批：金额已变动，流程完成 */
        FIN_APPROVED,
        /** 财务已拒绝：金额不变动（库存已变动，由业务线下处理） */
        FIN_REJECTED,
        /** 兼容旧数据（单阶段审批）*/
        APPROVED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kind kind;

    /** purchase_orders.id 或 sales_orders.id */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** 冗余存储，方便展示无需 JOIN */
    @Column(name = "order_no", length = 50)
    private String orderNo;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    /** JSON 数组：[{productId, productName, quantity, price}, ...] */
    @Column(name = "lines_json", nullable = false, columnDefinition = "TEXT")
    private String linesJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    /** 本次退货金额（提交时计算，財務审批时据此调整 paidAmount / receivedAmount） */
    @Column(name = "return_amount", precision = 15, scale = 2)
    private BigDecimal returnAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submit_user_id")
    @JsonIgnoreProperties({"roles", "password", "mfaLoginEnabled", "loginAlertEmailEnabled"})
    private User submitUser;

    /** 冗余存储提交人用户名，用于按提交人快速过滤 */
    @Column(name = "submit_username", length = 100)
    private String submitUsername;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ── 仓库审批 ──

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wh_approve_user_id")
    @JsonIgnoreProperties({"roles", "password", "mfaLoginEnabled", "loginAlertEmailEnabled"})
    private User whApproveUser;

    @Column(name = "wh_approved_at")
    private LocalDateTime whApprovedAt;

    @Column(name = "wh_reject_reason", length = 500)
    private String whRejectReason;

    /** 冗余存储，避免懒加载 */
    @Column(name = "wh_approve_username", length = 100)
    private String whApproveUsername;

    // ── 财务审批（复用原字段列名，减少 DDL 变更） ──

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approve_user_id")
    @JsonIgnoreProperties({"roles", "password", "mfaLoginEnabled", "loginAlertEmailEnabled"})
    private User finApproveUser;

    @Column(name = "approved_at")
    private LocalDateTime finApprovedAt;

    @Column(name = "fin_reject_reason", length = 500)
    private String finRejectReason;

    /** 冗余存储，避免懒加载 */
    @Column(name = "fin_approve_username", length = 100)
    private String finApproveUsername;
}
