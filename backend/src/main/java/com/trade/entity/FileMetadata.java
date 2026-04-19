package com.trade.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "file_metadata")
public class FileMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String fileId; // 文件唯一标识

    @Column(nullable = false, length = 255)
    private String fileName;

    @Column(length = 50)
    private String fileType;

    private Long fileSize;

    @Column(length = 500)
    private String filePath; // 存储路径

    @Column(length = 200)
    private String url; // 访问URL

    private String md5; // 文件MD5值

    @ManyToOne
    @JoinColumn(name = "uploader_id")
    @JsonIgnoreProperties({"roles", "password", "mfaLoginEnabled", "loginAlertEmailEnabled"})
    private User uploader;

    private String businessType; // 业务类型：product, contract, etc.

    private Long businessId; // 业务ID

    private String category; // 文件分类

    @ElementCollection
    private java.util.Set<String> tags; // 标签

    private Integer version = 1; // 版本号

    private Boolean isLatest = true; // 是否为最新版本

    private Long parentFileId; // 如果是历史版本，指向最新版本的文件ID

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private FileStatus status = FileStatus.ACTIVE;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    public enum FileStatus {
        ACTIVE, DELETED, EXPIRED
    }
}