package com.trade.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDTO {

    private Long id;

    private Long userId;

    private String username;

    private Long announcementId;

    private String announcementTitle;

    private String title;

    private String content;

    private String type;

    private Boolean isRead;

    private LocalDateTime readTime;

    private LocalDateTime createTime;
}
