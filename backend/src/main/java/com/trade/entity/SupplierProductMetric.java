package com.trade.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "supplier_product_metrics", uniqueConstraints = {
        @UniqueConstraint(name = "uk_supplier_product_period",
                columnNames = {"tenant_id", "supplier_id", "product_id", "period_type", "metric_year", "metric_month"})
})
public class SupplierProductMetric extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 10)
    private PeriodType periodType = PeriodType.MONTH;

    /** 指标所属年份 */
    @Column(name = "metric_year", nullable = false)
    private Integer year;

    /** 月指标 1-12；年指标为 0 */
    @Column(name = "metric_month", nullable = false)
    private Integer month;

    /** 订量指标 */
    @Column(name = "target_qty", precision = 10, scale = 3)
    private BigDecimal targetQty;

    @Column(length = 500)
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetricStatus status = MetricStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public enum PeriodType {
        YEAR, MONTH
    }

    public enum MetricStatus {
        ACTIVE, INACTIVE
    }
}
