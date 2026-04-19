package com.trade.controller;

import com.trade.dto.AnnouncementDTO;
import com.trade.entity.Announcement;
import com.trade.service.AnnouncementService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {
    private final AnnouncementService announcementService;

    /** 所有已登录用户均可查看公告列表 */
    @GetMapping
    public ApiResponse<Page<Announcement>> getAnnouncements(
            @RequestParam(required = false) String keyword,
            @PageableDefault(sort = "isTop", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Announcement> announcements = announcementService.getAnnouncements(keyword, pageable);
        return ApiResponse.success(announcements);
    }

    /** 所有已登录用户均可查看置顶公告（首页通知栏） */
    @GetMapping("/top")
    public ApiResponse<List<Announcement>> getTopAnnouncements() {
        List<Announcement> announcements = announcementService.getTopAnnouncements();
        return ApiResponse.success(announcements);
    }

    /** 发布公告：需要公告管理权限 */
    @PostMapping
    @PreAuthorize("hasAuthority('announcement:manage')")
    public ApiResponse<Announcement> createAnnouncement(@Valid @RequestBody AnnouncementDTO dto) {
        Announcement announcement = announcementService.createAnnouncement(dto);
        return ApiResponse.success(announcement);
    }

    /** 标记已读：所有已登录用户均可操作 */
    @PostMapping("/notifications/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long id) {
        announcementService.markAsRead(id);
        return ApiResponse.success(null);
    }
}
