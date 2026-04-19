package com.trade.controller;

import com.trade.dto.FinanceSummaryDTO;
import com.trade.repository.InventoryRepository;
import com.trade.repository.PurchaseOrderRepository;
import com.trade.repository.SalesOrderRepository;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 财务汇总：进货支出、销售收入、毛利、应付应收
 * 仅财务（purchase:pay / sales:collect）及管理员可访问
 */
@RestController
@RequestMapping("/finance/summary")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('purchase:pay','sales:collect','user:manage')")
public class FinanceSummaryController {

    private final PurchaseOrderRepository purchaseRepo;
    private final SalesOrderRepository salesRepo;
    private final InventoryRepository inventoryRepo;

    @GetMapping
    public ApiResponse<FinanceSummaryDTO> getSummary() {
        FinanceSummaryDTO dto = new FinanceSummaryDTO();

        // ── 采购汇总 ──
        List<Object[]> pList = purchaseRepo.sumFinance();
        Object[] p = pList.isEmpty() ? new Object[]{0, 0, 0} : pList.get(0);
        BigDecimal totalPurchase  = toBD(p[0]);
        BigDecimal totalPaid      = toBD(p[1]);
        BigDecimal totalUnpaid    = toBD(p[2]);
        dto.setTotalPurchaseAmount(totalPurchase);
        dto.setTotalPaidAmount(totalPaid);
        dto.setTotalUnpaidAmount(totalUnpaid);

        // ── 销售汇总 ──
        List<Object[]> sList = salesRepo.sumFinance();
        Object[] s = sList.isEmpty() ? new Object[]{0, 0, 0} : sList.get(0);
        BigDecimal totalSales     = toBD(s[0]);
        BigDecimal totalReceived  = toBD(s[1]);
        BigDecimal totalUnreceived = toBD(s[2]);
        dto.setTotalSalesAmount(totalSales);
        dto.setTotalReceivedAmount(totalReceived);
        dto.setTotalUnreceivedAmount(totalUnreceived);

        // ── 毛利 = 已收 - 已付 ──
        BigDecimal grossProfit = totalReceived.subtract(totalPaid);
        dto.setGrossProfit(grossProfit);
        if (totalReceived.compareTo(BigDecimal.ZERO) > 0) {
            dto.setGrossMarginPercent(
                    grossProfit.divide(totalReceived, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP));
        } else {
            dto.setGrossMarginPercent(BigDecimal.ZERO);
        }

        // ── 近 6 个月趋势 ──
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(5).withDayOfMonth(1);
        dto.setPurchaseByMonth(toRows(purchaseRepo.sumFinanceByMonth(sixMonthsAgo)));
        dto.setSalesByMonth(toRows(salesRepo.sumFinanceByMonth(sixMonthsAgo)));

        // ── 仓库资产 ──
        BigDecimal totalWarehouseAsset = toBD(inventoryRepo.sumTotalAsset());
        dto.setTotalWarehouseAsset(totalWarehouseAsset);
        List<FinanceSummaryDTO.WarehouseAssetRow> waRows = new ArrayList<>();
        for (Object[] r : inventoryRepo.sumAssetByWarehouse()) {
            waRows.add(new FinanceSummaryDTO.WarehouseAssetRow(
                    r[0] instanceof Number ? ((Number) r[0]).longValue() : Long.parseLong(r[0].toString()),
                    String.valueOf(r[1]),
                    toBD(r[2])
            ));
        }
        dto.setWarehouseAssets(waRows);

        return ApiResponse.success(dto);
    }

    private BigDecimal toBD(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Long || o instanceof Integer)
            return BigDecimal.valueOf(((Number) o).longValue());
        if (o instanceof Double || o instanceof Float)
            return BigDecimal.valueOf(((Number) o).doubleValue());
        try {
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private List<FinanceSummaryDTO.MonthRow> toRows(List<Object[]> rows) {
        List<FinanceSummaryDTO.MonthRow> list = new ArrayList<>();
        for (Object[] r : rows) {
            list.add(new FinanceSummaryDTO.MonthRow(
                    String.valueOf(r[0]),
                    toBD(r[1]),
                    toBD(r[2])
            ));
        }
        return list;
    }
}
