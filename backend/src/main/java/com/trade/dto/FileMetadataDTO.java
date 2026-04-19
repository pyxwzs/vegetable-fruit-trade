package com.trade.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class FileMetadataDTO {

    private Long id;

    private String fileId;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String fileSizeDisplay;

    private String filePath;

    private String url;

    private String md5;

    private Long uploaderId;

    private String uploaderName;

    private String businessType;

    private Long businessId;

    private String category;

    private Set<String> tags;

    private Integer version;

    private Boolean isLatest;

    private Long parentFileId;

    private String description;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
