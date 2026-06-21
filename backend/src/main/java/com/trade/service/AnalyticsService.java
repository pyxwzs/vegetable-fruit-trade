package com.trade.service;

import com.trade.dto.*;
import com.trade.entity.Customer;
import com.trade.entity.PurchaseOrder;
import com.trade.entity.SalesOrder;
import com.trade.entity.Supplier;
import com.trade.repository.*;
import com.trade.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// repositories injected via @RequiredArgsConstructor

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PurchasePaymentRepository purchasePaymentRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final CustomerRepository customerRepository;
    private final ExpenseRepository expenseRepository;
    private final SupplierRepository supplierRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public DailyReportDTO getDailyReport(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        BigDecimal realized = salesOrderRepository.sumRealizedSalesBetween(d, d);
        long newSales = salesOrderRepository.countOrdersOnDate(d);
        long newPurchase = purchaseOrderRepository.countOrdersOnDate(d);
        long pendingPurchase = purchaseOrderRepository.countByStatus(PurchaseOrder.OrderStatus.PENDING);
        long pendingSales = salesOrderRepository.countByStatus(SalesOrder.OrderStatus.PENDING);
        int lowStock = inventoryService.getLowStockProducts().size();
        return new DailyReportDTO(d, realized, newSales, newPurchase, pendingPurchase, pendingSales, lowStock, 0, 0);
    }

    public HomeSummaryDTO getHomeSummary() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        DailyReportDTO daily = getDailyReport(today);
        List<SalesTrendPointDTO> trend = buildFilledTrend(weekStart, today, salesOrderRepository.sumRealizedSalesByDay(weekStart, today));
        long sku = inventoryRepository.countDistinctProductsInStock();
        long activeCust = customerRepository.countByStatus(Customer.CustomerStatus.ACTIVE);
        return new HomeSummaryDTO(daily, trend, sku, activeCust);
    }

    public MonthlyReportDTO getMonthlyPurchaseBySupplier(Long supplierId, int year) {
        List<Object[]> rows = supplierId != null
                ? purchaseOrderRepository.monthlyStatsBySupplier(supplierId, year)
                : purchaseOrderRepository.monthlyStatsAllSuppliers(year);
        String name = "全部农户";
        if (supplierId != null) {
            name = supplierRepository.findById(supplierId).map(Supplier::getName).orElse("未知");
        }
        return buildMonthlyReport(supplierId, name, year, rows);
    }

    public MonthlyReportDTO getMonthlySalesByCustomer(Long customerId, int year) {
        List<Object[]> rows = customerId != null
                ? salesOrderRepository.monthlyStatsByCustomer(customerId, year)
                : salesOrderRepository.monthlyStatsAllCustomers(year);
        String name = "全部客户";
        if (customerId != null) {
            name = customerRepository.findById(customerId).map(Customer::getName).orElse("未知");
        }
        return buildMonthlyReport(customerId, name, year, rows);
    }

    public MonthlyOverviewDTO getMonthlyOverview(int year, int month) {
        BigDecimal purchaseTotal = nvl(purchaseOrderRepository.sumCompletedByMonth(year, month));
        BigDecimal salesTotal = nvl(salesOrderRepository.sumCompletedByMonth(year, month));
        BigDecimal paidToFarmers = nvl(purchasePaymentRepository.sumByMonth(year, month));
        BigDecimal collectedFromCustomers = nvl(salePaymentRepository.sumByMonth(year, month));
        BigDecimal otherExpenses = nvl(expenseRepository.sumByMonth(year, month));
        BigDecimal unpaidToFarmers = purchaseTotal.subtract(paidToFarmers).max(BigDecimal.ZERO);
        BigDecimal uncollectedFromCustomers = salesTotal.subtract(collectedFromCustomers).max(BigDecimal.ZERO);
        BigDecimal grossProfit = salesTotal.subtract(purchaseTotal);
        BigDecimal netProfit = grossProfit.subtract(otherExpenses);
        BigDecimal cashDifference = collectedFromCustomers.subtract(paidToFarmers);

        List<MonthlyOverviewDTO.BalanceRowDTO> farmerRanking = new ArrayList<>();
        for (Object[] r : purchaseOrderRepository.farmerBalanceRanking(year, month)) {
            MonthlyOverviewDTO.BalanceRowDTO row = new MonthlyOverviewDTO.BalanceRowDTO();
            row.setName(r[0] != null ? r[0].toString() : "");
            row.setTotal(toBigDecimal(r[1]));
            row.setSettled(toBigDecimal(r[2]));
            row.setUnpaid(toBigDecimal(r[1]).subtract(toBigDecimal(r[2])).max(BigDecimal.ZERO));
            farmerRanking.add(row);
        }

        List<MonthlyOverviewDTO.BalanceRowDTO> customerRanking = new ArrayList<>();
        for (Object[] r : salesOrderRepository.customerBalanceRanking(year, month)) {
            MonthlyOverviewDTO.BalanceRowDTO row = new MonthlyOverviewDTO.BalanceRowDTO();
            row.setName(r[0] != null ? r[0].toString() : "");
            row.setTotal(toBigDecimal(r[1]));
            row.setSettled(toBigDecimal(r[2]));
            row.setUnpaid(toBigDecimal(r[1]).subtract(toBigDecimal(r[2])).max(BigDecimal.ZERO));
            customerRanking.add(row);
        }

        // 商品毛利排行（将采购/销售金额按商品名合并）
        Map<String, BigDecimal> purchaseMap = new java.util.LinkedHashMap<>();
        Map<String, String> unitMap = new java.util.LinkedHashMap<>();
        for (Object[] r : purchaseOrderRepository.productPurchaseSumByMonth(year, month)) {
            String name = r[0] != null ? r[0].toString() : "";
            unitMap.put(name, r[1] != null ? r[1].toString() : "");
            purchaseMap.put(name, toBigDecimal(r[2]));
        }
        Map<String, BigDecimal> salesMap = new java.util.LinkedHashMap<>();
        for (Object[] r : salesOrderRepository.productSalesSumByMonth(year, month)) {
            String name = r[0] != null ? r[0].toString() : "";
            if (!unitMap.containsKey(name)) unitMap.put(name, r[1] != null ? r[1].toString() : "");
            salesMap.put(name, toBigDecimal(r[2]));
        }
        Set<String> allProducts = new java.util.LinkedHashSet<>();
        allProducts.addAll(purchaseMap.keySet());
        allProducts.addAll(salesMap.keySet());
        List<MonthlyOverviewDTO.ProductGrossDTO> productRanking = new ArrayList<>();
        for (String pName : allProducts) {
            BigDecimal pa = purchaseMap.getOrDefault(pName, BigDecimal.ZERO);
            BigDecimal sa = salesMap.getOrDefault(pName, BigDecimal.ZERO);
            BigDecimal gp = sa.subtract(pa);
            String margin = pa.compareTo(BigDecimal.ZERO) > 0
                    ? gp.multiply(new BigDecimal("100")).divide(pa, 1, java.math.RoundingMode.HALF_UP) + "%"
                    : "-";
            MonthlyOverviewDTO.ProductGrossDTO dto = new MonthlyOverviewDTO.ProductGrossDTO();
            dto.setProductName(pName);
            dto.setUnit(unitMap.getOrDefault(pName, ""));
            dto.setPurchaseAmount(pa);
            dto.setSalesAmount(sa);
            dto.setGrossProfit(gp);
            dto.setGrossMargin(margin);
            productRanking.add(dto);
        }
        productRanking.sort((a, b) -> b.getGrossProfit().compareTo(a.getGrossProfit()));

        MonthlyOverviewDTO dto = new MonthlyOverviewDTO();
        dto.setYear(year);
        dto.setMonth(month);
        dto.setPurchaseTotal(purchaseTotal);
        dto.setSalesTotal(salesTotal);
        dto.setGrossProfit(grossProfit);
        dto.setPaidToFarmers(paidToFarmers);
        dto.setUnpaidToFarmers(unpaidToFarmers);
        dto.setCollectedFromCustomers(collectedFromCustomers);
        dto.setUncollectedFromCustomers(uncollectedFromCustomers);
        dto.setCashDifference(cashDifference);
        dto.setOtherExpenses(otherExpenses);
        dto.setNetProfit(netProfit);
        dto.setFarmerUnpaidRanking(farmerRanking);
        dto.setCustomerUnreceivedRanking(customerRanking);
        dto.setProductGrossRanking(productRanking);
        return dto;
    }

    private BigDecimal nvl(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    public MonthItemsReportDTO getPurchaseItemsByMonth(Long supplierId, int year, int month) {
        List<Object[]> raw = supplierId != null
                ? purchaseOrderItemRepository.itemDetailBySupplier(supplierId, year, month)
                : purchaseOrderItemRepository.itemDetailAll(year, month);
        String name = supplierId != null
                ? supplierRepository.findById(supplierId).map(Supplier::getName).orElse("未知")
                : "全部农户";
        return buildItemsReport(supplierId, name, year, month, raw);
    }

    public MonthItemsReportDTO getSalesItemsByMonth(Long customerId, int year, int month) {
        List<Object[]> raw = customerId != null
                ? salesOrderItemRepository.itemDetailByCustomer(customerId, year, month)
                : salesOrderItemRepository.itemDetailAll(year, month);
        String name = customerId != null
                ? customerRepository.findById(customerId).map(Customer::getName).orElse("未知")
                : "全部客户";
        return buildItemsReport(customerId, name, year, month, raw);
    }

    public PartnerListReportDTO getPurchasePartners(int year, Integer month) {
        List<Object[]> raw = month != null
                ? purchaseOrderRepository.supplierStatsByMonth(year, month)
                : purchaseOrderRepository.supplierStatsByYear(year);
        return buildPartnerListReport(year, month, raw);
    }

    public PartnerListReportDTO getSalesPartners(int year, Integer month) {
        List<Object[]> raw = month != null
                ? salesOrderRepository.customerStatsByMonth(year, month)
                : salesOrderRepository.customerStatsByYear(year);
        return buildPartnerListReport(year, month, raw);
    }

    /** 供应商视角：各供应商提供的商品、数量与金额汇总 */
    public PartnerProductStatReportDTO getSupplierProductStats(int year, Integer month, Long supplierId) {
        int m = month != null ? month : 0;
        List<Object[]> raw = purchaseOrderItemRepository.supplierProductStats(year, m, supplierId);
        return buildPartnerProductStatReport(year, month, raw);
    }

    /** 客户视角：销售给各客户的商品、数量与金额汇总 */
    public PartnerProductStatReportDTO getCustomerProductStats(int year, Integer month, Long customerId) {
        int m = month != null ? month : 0;
        List<Object[]> raw = salesOrderItemRepository.customerProductStats(year, m, customerId);
        return buildPartnerProductStatReport(year, month, raw);
    }

    private PartnerProductStatReportDTO buildPartnerProductStatReport(int year, Integer month, List<Object[]> rawRows) {
        PartnerProductStatReportDTO report = new PartnerProductStatReportDTO();
        report.setYear(year);
        report.setMonth(month);
        if (rawRows == null || rawRows.isEmpty()) {
            return report;
        }

        Map<Long, PartnerProductGroupDTO> groupMap = new LinkedHashMap<>();
        BigDecimal grandAmount = BigDecimal.ZERO;
        BigDecimal grandQty = BigDecimal.ZERO;

        for (Object[] r : rawRows) {
            Long entityId = r[0] instanceof Number ? ((Number) r[0]).longValue() : null;
            String entityName = r[1] != null ? r[1].toString() : "";
            Long productId = r[2] instanceof Number ? ((Number) r[2]).longValue() : null;
            String productName = r[3] != null ? r[3].toString() : "";
            String unit = r[4] != null ? r[4].toString() : "";
            BigDecimal qty = toBigDecimal(r[5]);
            BigDecimal amount = toBigDecimal(r[6]);

            PartnerProductGroupDTO group = groupMap.computeIfAbsent(entityId, id -> {
                PartnerProductGroupDTO g = new PartnerProductGroupDTO();
                g.setEntityId(id);
                g.setEntityName(entityName);
                return g;
            });

            PartnerProductRowDTO row = new PartnerProductRowDTO();
            row.setProductId(productId);
            row.setProductName(productName);
            row.setUnit(unit);
            row.setQuantity(qty);
            row.setTotalAmount(amount);
            group.getProducts().add(row);
            group.setTotalAmount(group.getTotalAmount().add(amount));
            group.setTotalQuantity(group.getTotalQuantity().add(qty));
            grandAmount = grandAmount.add(amount);
            grandQty = grandQty.add(qty);
        }

        report.setGroups(new ArrayList<>(groupMap.values()));
        report.setGrandTotalAmount(grandAmount);
        report.setGrandTotalQuantity(grandQty);
        return report;
    }

    private PartnerListReportDTO buildPartnerListReport(int year, Integer month, List<Object[]> rawRows) {
        List<PartnerStatRowDTO> rows = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal settled = BigDecimal.ZERO;
        if (rawRows != null) {
            for (Object[] r : rawRows) {
                PartnerStatRowDTO row = new PartnerStatRowDTO();
                row.setEntityId(r[0] instanceof Number ? ((Number) r[0]).longValue() : null);
                row.setName(r[1] != null ? r[1].toString() : "");
                row.setOrderCount(r[2] instanceof Number ? ((Number) r[2]).longValue() : 0L);
                BigDecimal amt = toBigDecimal(r[3]);
                BigDecimal stl = toBigDecimal(r[4]);
                row.setTotalAmount(amt);
                row.setSettledAmount(stl);
                row.setPendingAmount(amt.subtract(stl).max(BigDecimal.ZERO));
                rows.add(row);
                total = total.add(amt);
                settled = settled.add(stl);
            }
        }
        PartnerListReportDTO dto = new PartnerListReportDTO();
        dto.setYear(year);
        dto.setMonth(month);
        dto.setRows(rows);
        dto.setTotalAmount(total);
        dto.setSettledAmount(settled);
        dto.setPendingAmount(total.subtract(settled).max(BigDecimal.ZERO));
        return dto;
    }

    private MonthItemsReportDTO buildItemsReport(Long entityId, String entityName, int year, int month, List<Object[]> raw) {
        List<OrderItemDetailDTO> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalQuantity = BigDecimal.ZERO;
        if (raw != null) {
            for (Object[] r : raw) {
                LocalDate date = r[0] instanceof LocalDate ? (LocalDate) r[0] : null;
                String partnerName = r[1] != null ? r[1].toString() : "";
                String orderNo = r[2] != null ? r[2].toString() : "";
                String paymentStatus = r[3] != null ? r[3].toString() : "";
                String productName = r[4] != null ? r[4].toString() : "";
                String unit = r[5] != null ? r[5].toString() : "";
                String spec = r[6] != null ? r[6].toString() : "";
                BigDecimal qty = toBigDecimal(r[7]);
                BigDecimal price = toBigDecimal(r[8]);
                BigDecimal amount = toBigDecimal(r[9]);
                items.add(new OrderItemDetailDTO(date, partnerName, orderNo, paymentStatus, productName, unit, spec, qty, price, amount));
                totalAmount = totalAmount.add(amount);
                totalQuantity = totalQuantity.add(qty);
            }
        }
        MonthItemsReportDTO dto = new MonthItemsReportDTO();
        dto.setEntityId(entityId);
        dto.setEntityName(entityName);
        dto.setYear(year);
        dto.setMonth(month);
        dto.setItems(items);
        dto.setTotalAmount(totalAmount);
        dto.setTotalQuantity(totalQuantity);
        return dto;
    }

    public DailyReportDetailDTO getDailyPurchaseDetail(Long supplierId, int year, int month) {
        List<Object[]> rows = supplierId != null
                ? purchaseOrderRepository.dailyStatsBySupplier(supplierId, year, month)
                : purchaseOrderRepository.dailyStatsAllSuppliers(year, month);
        String name = supplierId != null
                ? supplierRepository.findById(supplierId).map(Supplier::getName).orElse("未知")
                : "全部农户";
        return buildDailyDetail(supplierId, name, year, month, rows);
    }

    public DailyReportDetailDTO getDailySalesDetail(Long customerId, int year, int month) {
        List<Object[]> rows = customerId != null
                ? salesOrderRepository.dailyStatsByCustomer(customerId, year, month)
                : salesOrderRepository.dailyStatsAllCustomers(year, month);
        String name = customerId != null
                ? customerRepository.findById(customerId).map(Customer::getName).orElse("未知")
                : "全部客户";
        return buildDailyDetail(customerId, name, year, month, rows);
    }

    private DailyReportDetailDTO buildDailyDetail(Long entityId, String entityName, int year, int month, List<Object[]> rawRows) {
        List<DailyStatRowDTO> rows = new ArrayList<>();
        BigDecimal monthTotal = BigDecimal.ZERO;
        BigDecimal monthSettled = BigDecimal.ZERO;
        if (rawRows != null) {
            for (Object[] r : rawRows) {
                LocalDate date = r[0] instanceof LocalDate ? (LocalDate) r[0] : null;
                long cnt = r[1] instanceof Number ? ((Number) r[1]).longValue() : 0L;
                BigDecimal total = toBigDecimal(r[2]);
                BigDecimal settled = toBigDecimal(r[3]);
                BigDecimal pending = total.subtract(settled).max(BigDecimal.ZERO);
                rows.add(new DailyStatRowDTO(date, cnt, total, settled, pending));
                monthTotal = monthTotal.add(total);
                monthSettled = monthSettled.add(settled);
            }
        }
        DailyReportDetailDTO dto = new DailyReportDetailDTO();
        dto.setEntityId(entityId);
        dto.setEntityName(entityName);
        dto.setYear(year);
        dto.setMonth(month);
        dto.setRows(rows);
        dto.setMonthTotalAmount(monthTotal);
        dto.setMonthSettledAmount(monthSettled);
        dto.setMonthPendingAmount(monthTotal.subtract(monthSettled).max(BigDecimal.ZERO));
        return dto;
    }

    private MonthlyReportDTO buildMonthlyReport(Long entityId, String entityName, int year, List<Object[]> rawRows) {
        Map<Integer, Object[]> map = new HashMap<>();
        if (rawRows != null) {
            for (Object[] r : rawRows) {
                int m = ((Number) r[0]).intValue();
                map.put(m, r);
            }
        }
        List<MonthlyStatRowDTO> rows = new ArrayList<>();
        BigDecimal yearTotal = BigDecimal.ZERO;
        BigDecimal yearSettled = BigDecimal.ZERO;
        for (int m = 1; m <= 12; m++) {
            Object[] r = map.get(m);
            long cnt = r != null ? ((Number) r[1]).longValue() : 0L;
            BigDecimal total = r != null ? toBigDecimal(r[2]) : BigDecimal.ZERO;
            if (cnt <= 0 && total.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal settled = r != null ? toBigDecimal(r[3]) : BigDecimal.ZERO;
            BigDecimal pending = total.subtract(settled).max(BigDecimal.ZERO);
            rows.add(new MonthlyStatRowDTO(m, cnt, total, settled, pending));
            yearTotal = yearTotal.add(total);
            yearSettled = yearSettled.add(settled);
        }
        MonthlyReportDTO dto = new MonthlyReportDTO();
        dto.setEntityId(entityId);
        dto.setEntityName(entityName);
        dto.setYear(year);
        dto.setRows(rows);
        dto.setYearTotalAmount(yearTotal);
        dto.setYearSettledAmount(yearSettled);
        dto.setYearPendingAmount(yearTotal.subtract(yearSettled).max(BigDecimal.ZERO));
        return dto;
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) {
            return BigDecimal.ZERO;
        }
        if (o instanceof BigDecimal) {
            return (BigDecimal) o;
        }
        if (o instanceof BigInteger) {
            return new BigDecimal((BigInteger) o);
        }
        if (o instanceof Number) {
            double d = ((Number) o).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.valueOf(d);
        }
        if (o instanceof byte[]) {
            try {
                return new BigDecimal(new String((byte[]) o, StandardCharsets.UTF_8).trim());
            } catch (NumberFormatException | ArithmeticException e) {
                return BigDecimal.ZERO;
            }
        }
        if (o instanceof char[]) {
            try {
                return new BigDecimal(new String((char[]) o).trim());
            } catch (NumberFormatException | ArithmeticException e) {
                return BigDecimal.ZERO;
            }
        }
        try {
            String s = o.toString().trim();
            if (s.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(s);
        } catch (NumberFormatException | ArithmeticException e) {
            return BigDecimal.ZERO;
        }
    }

    private static Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Long) {
            return (Long) o;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal trendPercent(BigDecimal current, BigDecimal previous) {
        if (previous == null) {
            previous = BigDecimal.ZERO;
        }
        if (current == null) {
            current = BigDecimal.ZERO;
        }
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("100.0") : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);
    }

    private static LocalDate[] resolveRange(String rangeType) {
        LocalDate today = LocalDate.now();
        String rt = rangeType == null ? "month" : rangeType.trim().toLowerCase();
        switch (rt) {
            case "week":
                return new LocalDate[]{today.minusDays(6), today};
            case "month":
                return new LocalDate[]{today.withDayOfMonth(1), today};
            case "quarter": {
                int m = today.getMonthValue();
                int startMonth = ((m - 1) / 3) * 3 + 1;
                return new LocalDate[]{LocalDate.of(today.getYear(), startMonth, 1), today};
            }
            case "year":
                return new LocalDate[]{LocalDate.of(today.getYear(), 1, 1), today};
            default:
                return new LocalDate[]{today.withDayOfMonth(1), today};
        }
    }

    private static List<SalesTrendPointDTO> buildFilledTrend(LocalDate start, LocalDate end, List<Object[]> rows) {
        Map<LocalDate, BigDecimal> map = new HashMap<>();
        if (rows != null) {
            for (Object[] row : rows) {
                if (row != null && row.length > 1 && row[0] instanceof LocalDate) {
                    map.put((LocalDate) row[0], toBigDecimal(row[1]));
                }
            }
        }
        long len = ChronoUnit.DAYS.between(start, end) + 1;
        return IntStream.range(0, (int) len)
                .mapToObj(i -> {
                    LocalDate d = start.plusDays(i);
                    return new SalesTrendPointDTO(d, map.getOrDefault(d, BigDecimal.ZERO));
                })
                .collect(Collectors.toList());
    }

    public Map<String, BigDecimal> getYearlyBalance(int year) {
        BigDecimal unpaid = nvl(purchaseOrderRepository.sumUnpaidByYear(year));
        BigDecimal uncollected = nvl(salesOrderRepository.sumUncollectedByYear(year));
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        result.put("unpaidToFarmers", unpaid);
        result.put("uncollectedFromCustomers", uncollected);
        return result;
    }
}
