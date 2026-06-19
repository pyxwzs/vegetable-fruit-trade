package com.trade.controller;

import com.trade.dto.*;
import com.trade.dto.MonthlyReportDTO;
import com.trade.service.AnalyticsService;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /** 仪表盘首页摘要 */
    @GetMapping("/home-summary")
    public ApiResponse<HomeSummaryDTO> homeSummary() {
        return ApiResponse.success(analyticsService.getHomeSummary());
    }

    @GetMapping("/monthly-purchase")
    public ApiResponse<MonthlyReportDTO> monthlyPurchase(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(defaultValue = "0") int year) {
        int y = year > 0 ? year : LocalDate.now().getYear();
        return ApiResponse.success(analyticsService.getMonthlyPurchaseBySupplier(supplierId, y));
    }

    @GetMapping("/monthly-sales")
    public ApiResponse<MonthlyReportDTO> monthlySales(
            @RequestParam(required = false) Long customerId,
            @RequestParam(defaultValue = "0") int year) {
        int y = year > 0 ? year : LocalDate.now().getYear();
        return ApiResponse.success(analyticsService.getMonthlySalesByCustomer(customerId, y));
    }

    @GetMapping("/purchase-items")
    public ApiResponse<MonthItemsReportDTO> purchaseItems(
            @RequestParam(required = false) Long supplierId,
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.success(analyticsService.getPurchaseItemsByMonth(supplierId, year, month));
    }

    @GetMapping("/sales-items")
    public ApiResponse<MonthItemsReportDTO> salesItems(
            @RequestParam(required = false) Long customerId,
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.success(analyticsService.getSalesItemsByMonth(customerId, year, month));
    }

    @GetMapping("/daily-purchase")
    public ApiResponse<DailyReportDetailDTO> dailyPurchase(
            @RequestParam(required = false) Long supplierId,
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.success(analyticsService.getDailyPurchaseDetail(supplierId, year, month));
    }

    @GetMapping("/daily-sales")
    public ApiResponse<DailyReportDetailDTO> dailySales(
            @RequestParam(required = false) Long customerId,
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.success(analyticsService.getDailySalesDetail(customerId, year, month));
    }

    @GetMapping("/purchase-partners")
    public ApiResponse<PartnerListReportDTO> purchasePartners(
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {
        return ApiResponse.success(analyticsService.getPurchasePartners(year, month));
    }

    @GetMapping("/sales-partners")
    public ApiResponse<PartnerListReportDTO> salesPartners(
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {
        return ApiResponse.success(analyticsService.getSalesPartners(year, month));
    }

    @GetMapping("/monthly-overview")
    public ApiResponse<MonthlyOverviewDTO> monthlyOverview(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        LocalDate now = LocalDate.now();
        int y = year > 0 ? year : now.getYear();
        int m = month > 0 ? month : now.getMonthValue();
        return ApiResponse.success(analyticsService.getMonthlyOverview(y, m));
    }

    /** 全年累计未付/未收款 */
    @GetMapping("/yearly-balance")
    public ApiResponse<java.util.Map<String, java.math.BigDecimal>> yearlyBalance(
            @RequestParam(defaultValue = "0") int year) {
        LocalDate now = LocalDate.now();
        int y = year > 0 ? year : now.getYear();
        return ApiResponse.success(analyticsService.getYearlyBalance(y));
    }
}
