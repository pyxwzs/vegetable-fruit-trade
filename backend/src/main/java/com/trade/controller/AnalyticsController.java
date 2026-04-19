package com.trade.controller;

import com.trade.dto.*;
import com.trade.service.AnalyticsService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 经营分析：完整分析页仅管理员可访问；home-summary 供仪表盘全角色使用。
 */
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /** 仪表盘首页摘要，所有已登录角色均可调用 */
    @GetMapping("/home-summary")
    @PreAuthorize("hasAnyAuthority('purchase:view','sales:view','inventory:view','purchase:pay','sales:collect')")
    public ApiResponse<HomeSummaryDTO> homeSummary() {
        return ApiResponse.success(analyticsService.getHomeSummary());
    }

    @GetMapping("/daily-report")
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<DailyReportDTO> dailyReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(analyticsService.getDailyReport(date));
    }

    @GetMapping("/overview-kpi")
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<AnalyticsKpiDTO> overviewKpi(@RequestParam(defaultValue = "month") String range) {
        return ApiResponse.success(analyticsService.getOverviewKpi(range));
    }

    @GetMapping("/sales-trend")
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<List<SalesTrendPointDTO>> salesTrend(@RequestParam(defaultValue = "month") String range) {
        return ApiResponse.success(analyticsService.getSalesTrend(range));
    }

    @GetMapping("/product-ranking")
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<List<ProductSalesRankDTO>> productRanking(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(analyticsService.getProductRanking(range, limit));
    }

    @GetMapping("/profit-summary")
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<ProfitSummaryDTO> profitSummary(@RequestParam(defaultValue = "month") String range) {
        return ApiResponse.success(analyticsService.getProfitSummary(range));
    }

    @GetMapping("/replenishment")
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<List<ReplenishmentSuggestionDTO>> replenishment() {
        return ApiResponse.success(analyticsService.getReplenishmentSuggestions());
    }

    @GetMapping("/customer-ranking")
    @PreAuthorize("hasAuthority('user:manage')")
    public ApiResponse<List<CustomerValueRowDTO>> customerRanking(
            @RequestParam(defaultValue = "month") String range,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(analyticsService.getCustomerRanking(range, limit));
    }
}
