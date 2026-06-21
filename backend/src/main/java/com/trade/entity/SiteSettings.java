package com.trade.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "site_settings", uniqueConstraints = @UniqueConstraint(columnNames = "tenant_id"))
public class SiteSettings extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_name", nullable = false, length = 100)
    private String siteName = "果蔬批发";

    /** 相对路径如 /uploads/1/site/logo.png，或外链 https://... */
    @Column(name = "logo_path", length = 500)
    private String logoPath;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
