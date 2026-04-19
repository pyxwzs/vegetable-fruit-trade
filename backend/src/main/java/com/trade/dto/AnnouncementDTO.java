package com.trade.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class AnnouncementDTO {
    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private Set<String> targetRoles;
    private String priority;
    private Boolean isTop;
    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private Boolean isTimed;
    private String attachments;
}
