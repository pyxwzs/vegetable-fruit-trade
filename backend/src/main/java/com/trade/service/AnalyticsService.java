package com.trade.service;

import com.trade.dto.*;
import com.trade.entity.Customer;
import com.trade.entity.PurchaseOrder;
import com.trade.entity.SalesOrder;
import com.trade.repository.*;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final BigDecimal REPLENISH_TARGET = new BigDecimal("30");
    private static final BigDecimal LOW_STOCK_THRESHOLD = new BigDecimal("10");

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final CustomerRepository customerRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;
    private final CustomerService customerService;
    private final UserService userService;

    public DailyReportDTO getDailyReport(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        Long scope = userService.resolveBizDataScopeUserId();
        BigDecimal realized = salesOrderRepository.sumRealizedSalesBetween(d, d, scope);
        long newSales = salesOrderRepository.countOrdersOnDate(d, scope);
        long newPurchase = purchaseOrderRepository.countOrdersOnDate(d, scope);
        long pendingPurchase = purchaseOrderRepository.countByStatusScoped(PurchaseOrder.OrderStatus.PENDING, scope);
        long pendingSales = salesOrderRepository.countByStatusScoped(SalesOrder.OrderStatus.PENDING, scope);
        int lowStock = inventoryService.getLowStockProducts().size();
        int expiring = inventoryService.getExpiringProducts().size();
        int creditWarn = customerService.listCreditWarnings().size();
        return new DailyReportDTO(d, realized, newSales, newPurchase, pendingPurchase, pendingSales, lowStock, expiring, creditWarn);
    }

    public HomeSummaryDTO getHomeSummary() {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        Long scope = userService.resolveBizDataScopeUserId();
        DailyReportDTO daily = getDailyReport(today);
        List<SalesTrendPointDTO> trend = buildFilledTrend(weekStart, today, salesOrderRepository.sumRealizedSalesByDay(weekStart, today, scope));
        long sku = inventoryRepository.countDistinctProductsInStock();
        long activeCust = customerRepository.countByStatus(Customer.CustomerStatus.ACTIVE);
        return new HomeSummaryDTO(daily, trend, sku, activeCust);
    }

    public AnalyticsKpiDTO getOverviewKpi(String rangeType) {
        LocalDate[] cur = resolveRange(rangeType);
        LocalDate[] prev = previousRange(cur[0], cur[1]);
        Long scope = userService.resolveBizDataScopeUserId();

        BigDecimal salesCur = salesOrderRepository.sumRealizedSalesBetween(cur[0], cur[1], scope);
        BigDecimal salesPrev = salesOrderRepository.sumRealizedSalesBetween(prev[0], prev[1], scope);

        Object[] profitCur = salesOrderItemRepository.sumRevenueAndEstimatedCost(cur[0], cur[1], scope);
        Object[] profitPrev = salesOrderItemRepository.sumRevenueAndEstimatedCost(prev[0], prev[1], scope);
        BigDecimal gpCur = grossProfitFromRow(profitCur);
        BigDecimal gpPrev = grossProfitFromRow(profitPrev);

        long ordCur = salesOrderRepository.countOrdersBetween(cur[0], cur[1], scope);
        long ordPrev = salesOrderRepository.countOrdersBetween(prev[0], prev[1], scope);

        long custCur = salesOrderRepository.countDistinctCustomersWithOrders(cur[0], cur[1], scope);
        long custPrev = salesOrderRepository.countDistinctCustomersWithOrders(prev[0], prev[1], scope);

        return new AnalyticsKpiDTO(
                salesCur,
                trendPercent(salesCur, salesPrev),
                gpCur,
                trendPercent(gpCur, gpPrev),
                ordCur,
                trendPercent(BigDecimal.valueOf(ordCur), BigDecimal.valueOf(ordPrev)),
                custCur,
                trendPercent(BigDecimal.valueOf(custCur), BigDecimal.valueOf(custPrev))
        );
    }

    public List<SalesTrendPointDTO> getSalesTrend(String rangeType) {
        LocalDate[] r = resolveRange(rangeType);
        Long scope = userService.resolveBizDataScopeUserId();
        List<Object[]> rows = salesOrderRepository.sumRealizedSalesByDay(r[0], r[1], scope);
        return buildFilledTrend(r[0], r[1], rows);
    }

    public List<ProductSalesRankDTO> getProductRanking(String rangeType, int limit) {
        LocalDate[] r = resolveRange(rangeType);
        Long scope = userService.resolveBizDataScopeUserId();
        List<Object[]> rows = salesOrderItemRepository.sumSalesByProduct(r[0], r[1], scope);
        int n = Math.min(limit > 0 ? limit : 10, rows.size());
        List<ProductSalesRankDTO> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Object[] row = rows.get(i);
            out.add(new ProductSalesRankDTO(
                    toLong(row[0]),
                    row[1] != null ? row[1].toString() : "",
                    toBigDecimal(row[2])));
        }
        return out;
    }

    public ProfitSummaryDTO getProfitSummary(String rangeType) {
        LocalDate[] r = resolveRange(rangeType);
        Long scope = userService.resolveBizDataScopeUserId();
        Object[] rc = salesOrderItemRepository.sumRevenueAndEstimatedCost(r[0], r[1], scope);
        BigDecimal revenue = toBigDecimal(rc != null && rc.length > 0 ? rc[0] : null);
        BigDecimal cost = toBigDecimal(rc != null && rc.length > 1 ? rc[1] : null);
        BigDecimal profit = revenue.subtract(cost);
        BigDecimal margin = revenue.compareTo(BigDecimal.ZERO) > 0
                ? profit.divide(revenue, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Object[]> catRows = salesOrderItemRepository.sumSalesByCategory(r[0], r[1], scope);
        List<ProfitSummaryDTO.CategorySalesDTO> cats = new ArrayList<>();
        for (Object[] row : catRows) {
            String name = row[0] != null ? row[0].toString() : "未分类";
            cats.add(new ProfitSummaryDTO.CategorySalesDTO(name, toBigDecimal(row.length > 1 ? row[1] : null)));
        }

        ProfitSummaryDTO dto = new ProfitSummaryDTO();
        dto.setRevenue(revenue);
        dto.setEstimatedCost(cost);
        dto.setGrossProfit(profit);
        dto.setGrossMargin(margin);
        dto.setSalesByCategory(cats);
        return dto;
    }

    public List<ReplenishmentSuggestionDTO> getReplenishmentSuggestions() {
        List<Object[]> rows = inventoryRepository.findProductsBelowAvailableThreshold(LOW_STOCK_THRESHOLD);
        List<ReplenishmentSuggestionDTO> out = new ArrayList<>();
        for (Object[] row : rows) {
            BigDecimal avail = toBigDecimal(row.length > 3 ? row[3] : null);
            BigDecimal suggest = REPLENISH_TARGET.subtract(avail).max(BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
            out.add(new ReplenishmentSuggestionDTO(
                    toLong(row[0]),
                    row[1] != null ? row[1].toString() : "",
                    row[2] != null ? row[2].toString() : "",
                    avail,
                    suggest
            ));
        }
        out.sort(Comparator.comparing(ReplenishmentSuggestionDTO::getCurrentAvailableTotal));
        return out;
    }

    public List<CustomerValueRowDTO> getCustomerRanking(String rangeType, int limit) {
        LocalDate[] r = resolveRange(rangeType);
        Long scope = userService.resolveBizDataScopeUserId();
        List<Object[]> rows = salesOrderItemRepository.customerValueStats(r[0], r[1], scope);
        int n = Math.min(limit > 0 ? limit : 20, rows.size());
        List<CustomerValueRowDTO> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Object[] row = rows.get(i);
            BigDecimal rev = toBigDecimal(row.length > 4 ? row[4] : null);
            BigDecimal cost = toBigDecimal(row.length > 5 ? row[5] : null);
            BigDecimal gp = rev.subtract(cost);
            BigDecimal margin = rev.compareTo(BigDecimal.ZERO) > 0
                    ? gp.divide(rev, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            Object clObj = row[2];
            String creditStr = "";
            if (clObj instanceof Customer.CreditLevel) {
                creditStr = ((Customer.CreditLevel) clObj).name();
            } else if (clObj != null) {
                creditStr = clObj.toString();
            }
            out.add(new CustomerValueRowDTO(
                    i + 1,
                    toLong(row[0]),
                    row[1] != null ? row[1].toString() : "",
                    creditStr,
                    row[3] instanceof Number ? ((Number) row[3]).longValue() : 0L,
                    rev,
                    cost,
                    gp,
                    margin
            ));
        }
        return out;
    }

    private static BigDecimal grossProfitFromRow(Object[] rc) {
        if (rc == null || rc.length < 2) {
            return BigDecimal.ZERO;
        }
        return toBigDecimal(rc[0]).subtract(toBigDecimal(rc[1]));
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

    private static LocalDate[] previousRange(LocalDate start, LocalDate end) {
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        LocalDate prevEnd = start.minusDays(1);
        LocalDate prevStart = prevEnd.minusDays(days - 1);
        return new LocalDate[]{prevStart, prevEnd};
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
}
