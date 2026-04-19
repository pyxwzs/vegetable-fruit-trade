package com.trade.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "product_price_history")
public class ProductPriceHistory {

    public enum ChangeSource {
        /** 后台手工编辑 */
        MANUAL,
        /** Excel 批量导入 */
        IMPORT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public Long getProductId() {
        return product != null ? product.getId() : null;
    }

    @Column(precision = 10, scale = 2)
    private BigDecimal prevPurchasePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal prevSalePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal newPurchasePrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal newSalePrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeSource source = ChangeSource.MANUAL;

    @Column(length = 64)
    private String operatorUsername;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
