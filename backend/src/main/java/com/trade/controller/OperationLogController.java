package com.trade.controller;

import com.trade.entity.OperationLog;
import com.trade.security.SecurityUtils;
import com.trade.service.LogService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {

    private final LogService logService;

    /**
     * 查询操作日志：
     * <ul>
     *   <li>拥有 {@code log:view} 权限（管理员）：可按任意用户名筛选，查看全员日志。</li>
     *   <li>其他已登录用户：强制只返回当前账号的日志，忽略前端传入的 username 参数。</li>
     * </ul>
     */
    @GetMapping
    public ApiResponse<Page<OperationLog>> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {

        // 无 log:view 权限时，强制只能查自己的日志
        if (!SecurityUtils.hasAuthority("log:view")) {
            username = SecurityUtils.currentUsername();
        }

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : null;
        return ApiResponse.success(logService.getLogs(username, module, start, end, success, pageable));
    }
}
