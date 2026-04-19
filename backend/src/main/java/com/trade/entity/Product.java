package com.trade.entity;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String productCode;

    /** 商超条码等，可与商品编码不同；为空则仅按商品编码检索 */
    @Column(unique = true, length = 64)
    private String barcode;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(length = 20)
    private String unit; // 计量单位：斤、箱、个等

    private String specification; // 规格

    @Column(precision = 10, scale = 2)
    private BigDecimal purchasePrice; // 采购参考价

    @Column(precision = 10, scale = 2)
    private BigDecimal salePrice; // 销售参考价

    private Integer shelfLife; // 保质期（天）

    private String imageUrl; // 商品图片

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.ENABLED;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum ProductStatus {
        ENABLED, DISABLED
    }
}