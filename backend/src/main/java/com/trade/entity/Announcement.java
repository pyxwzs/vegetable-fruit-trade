package com.trade.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "announcements")
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne
    @JoinColumn(name = "publisher_id")
    @JsonIgnoreProperties({"roles", "password", "mfaLoginEnabled", "loginAlertEmailEnabled"})
    private User publisher;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "announcement_target_roles",
            joinColumns = @JoinColumn(name = "announcement_id"))
    @Column(name = "role_name")
    private Set<String> targetRoles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.NORMAL;

    private Boolean isTop = false; // 是否置顶

    private LocalDateTime publishTime; // 发布时间

    private LocalDateTime expireTime; // 过期时间

    private Boolean isTimed = false; // 是否定时发布

    private String attachments; // 附件，JSON格式存储

    @Enumerated(EnumType.STRING)
    private AnnouncementStatus status = AnnouncementStatus.DRAFT;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }

    public enum AnnouncementStatus {
        DRAFT, PUBLISHED, EXPIRED
    }
}