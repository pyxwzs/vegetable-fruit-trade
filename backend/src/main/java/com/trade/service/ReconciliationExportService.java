package com.trade.service;

import com.trade.dto.ExportFileDTO;
import com.trade.dto.MonthItemsReportDTO;
import com.trade.dto.OrderItemDetailDTO;
import com.trade.dto.PartnerListReportDTO;
import com.trade.dto.PartnerStatRowDTO;
import com.trade.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReconciliationExportService {

    private final AnalyticsService analyticsService;

    public ExportFileDTO exportPurchasePartners(int year, Integer month) {
        PartnerListReportDTO report = analyticsService.getPurchasePartners(year, month);
        String period = month != null ? year + "年" + month + "月" : year + "年";
        return exportPartnerList(report, "purchase", period);
    }

    public ExportFileDTO exportSalesPartners(int year, Integer month) {
        PartnerListReportDTO report = analyticsService.getSalesPartners(year, month);
        String period = month != null ? year + "年" + month + "月" : year + "年";
        return exportPartnerList(report, "sales", period);
    }

    public ExportFileDTO exportPurchaseMonth(Long supplierId, int year, int month) {
        if (supplierId == null) throw new BusinessException("请选择供应商");
        MonthItemsReportDTO report = analyticsService.getPurchaseItemsByMonth(supplierId, year, month);
        return exportMonthItems(report.getItems(), report.getEntityName(), year, month, "purchase");
    }

    public ExportFileDTO exportSalesMonth(Long customerId, int year, int month) {
        if (customerId == null) throw new BusinessException("请选择客户");
        MonthItemsReportDTO report = analyticsService.getSalesItemsByMonth(customerId, year, month);
        return exportMonthItems(report.getItems(), report.getEntityName(), year, month, "sales");
    }

    public ExportFileDTO exportPurchaseYear(Long supplierId, int year) {
        if (supplierId == null) throw new BusinessException("请选择供应商");
        String entityName = analyticsService.getPurchaseItemsByMonth(supplierId, year, 1).getEntityName();
        return exportYearItems(entityName, year, "purchase", (m) ->
                analyticsService.getPurchaseItemsByMonth(supplierId, year, m).getItems());
    }

    public ExportFileDTO exportSalesYear(Long customerId, int year) {
        if (customerId == null) throw new BusinessException("请选择客户");
        String entityName = analyticsService.getSalesItemsByMonth(customerId, year, 1).getEntityName();
        return exportYearItems(entityName, year, "sales", (m) ->
                analyticsService.getSalesItemsByMonth(customerId, year, m).getItems());
    }

    private ExportFileDTO exportPartnerList(PartnerListReportDTO report, String type, String periodLabel) {
        List<PartnerStatRowDTO> rows = report.getRows() != null ? report.getRows() : List.of();
        if (rows.isEmpty()) throw new BusinessException("暂无数据");
        boolean purchase = "purchase".equals(type);
        String label = purchase ? "采购" : "销售";
        String settledLabel = purchase ? "已付" : "已收";
        String pendingLabel = purchase ? "未付" : "未收";

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("汇总");
            CellStyle center = centerStyle(wb);
            int r = 0;
            Row titleRow = sheet.createRow(r++);
            createCell(titleRow, 0, periodLabel + label + "对账汇总", center);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            Row head = sheet.createRow(r++);
            String[] headers = {"名称", "单数", label + "总额(元)", settledLabel + "(元)", pendingLabel + "(元)"};
            for (int i = 0; i < headers.length; i++) createCell(head, i, headers[i], center);

            long totalOrders = 0;
            BigDecimal totalAmt = BigDecimal.ZERO;
            BigDecimal totalSettled = BigDecimal.ZERO;
            BigDecimal totalPending = BigDecimal.ZERO;
            for (PartnerStatRowDTO row : rows) {
                Row data = sheet.createRow(r++);
                createCell(data, 0, row.getName(), center);
                createCell(data, 1, row.getOrderCount(), center);
                createCell(data, 2, money(row.getTotalAmount()), center);
                createCell(data, 3, money(row.getSettledAmount()), center);
                createCell(data, 4, money(row.getPendingAmount()), center);
                totalOrders += row.getOrderCount();
                totalAmt = totalAmt.add(nvl(row.getTotalAmount()));
                totalSettled = totalSettled.add(nvl(row.getSettledAmount()));
                totalPending = totalPending.add(nvl(row.getPendingAmount()));
            }
            Row sum = sheet.createRow(r);
            createCell(sum, 0, "合计", center);
            createCell(sum, 1, totalOrders, center);
            createCell(sum, 2, money(totalAmt), center);
            createCell(sum, 3, money(totalSettled), center);
            createCell(sum, 4, money(totalPending), center);

            sheet.setColumnWidth(0, 16 * 256);
            for (int i = 1; i <= 4; i++) sheet.setColumnWidth(i, 14 * 256);
            wb.write(out);
            return new ExportFileDTO(out.toByteArray(), periodLabel + label + "对账汇总.xlsx");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("导出失败");
        }
    }

    private ExportFileDTO exportMonthItems(List<OrderItemDetailDTO> items, String entityName,
                                           int year, int month, String type) {
        MonthSheetBuilt built = buildMonthSheet(items);
        if (!built.isHasData()) throw new BusinessException("暂无数据");
        String label = "purchase".equals(type) ? "采购" : "销售";
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(month + "月");
            writeMonthSheet(sheet, wb, built);
            wb.write(out);
            return new ExportFileDTO(out.toByteArray(),
                    entityName + "_" + year + "年" + month + "月" + label + "对账.xlsx");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("导出失败");
        }
    }

    private ExportFileDTO exportYearItems(String entityName, int year, String type,
                                          MonthItemsLoader loader) {
        String label = "purchase".equals(type) ? "采购" : "销售";
        List<MonthSheetBuilt> monthSheets = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            MonthSheetBuilt built = buildMonthSheet(loader.load(m));
            if (built.isHasData()) {
                built.setMonth(m);
                monthSheets.add(built);
            }
        }
        if (monthSheets.isEmpty()) throw new BusinessException("暂无数据");

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle center = centerStyle(wb);
            Sheet summary = wb.createSheet("年度汇总");
            int r = 0;
            Row t = summary.createRow(r++);
            createCell(t, 0, entityName + " · " + year + "年" + label + "月度汇总", center);
            summary.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
            Row h = summary.createRow(r++);
            createCell(h, 0, "月份", center);
            createCell(h, 1, "金额(元)", center);
            BigDecimal yearTotal = BigDecimal.ZERO;
            for (MonthSheetBuilt ms : monthSheets) {
                Row row = summary.createRow(r++);
                createCell(row, 0, ms.getMonth() + "月", center);
                createCell(row, 1, money(ms.getMonthTotal()), center);
                yearTotal = yearTotal.add(ms.getMonthTotal());
            }
            Row total = summary.createRow(r);
            createCell(total, 0, "总计", center);
            createCell(total, 1, money(yearTotal), center);
            summary.setColumnWidth(0, 10 * 256);
            summary.setColumnWidth(1, 14 * 256);

            for (MonthSheetBuilt ms : monthSheets) {
                Sheet sheet = wb.createSheet(ms.getMonth() + "月");
                writeMonthSheet(sheet, wb, ms);
            }
            wb.write(out);
            return new ExportFileDTO(out.toByteArray(), entityName + "_" + year + "年" + label + "对账.xlsx");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("导出失败");
        }
    }

    private void writeMonthSheet(Sheet sheet, Workbook wb, MonthSheetBuilt built) {
        CellStyle center = centerStyle(wb);
        int r = 0;
        for (List<Object> rowData : built.getRows()) {
            Row row = sheet.createRow(r++);
            for (int c = 0; c < rowData.size(); c++) {
                Object val = rowData.get(c);
                if (val instanceof Number num) {
                    createCell(row, c, num.doubleValue(), center);
                } else {
                    createCell(row, c, val == null ? "" : String.valueOf(val), center);
                }
            }
        }
        for (int i = 0; i < built.getProductCount(); i++) {
            int startCol = 1 + i * 3;
            if (startCol + 2 <= built.getMaxCol()) {
                sheet.addMergedRegion(new CellRangeAddress(0, 0, startCol, startCol + 2));
            }
        }
        int totalCol = built.getMaxCol();
        sheet.addMergedRegion(new CellRangeAddress(0, 1, totalCol, totalCol));
        sheet.setColumnWidth(0, 12 * 256);
        for (int i = 1; i <= built.getProductCount() * 3; i++) {
            sheet.setColumnWidth(i, (i % 3 == 2 ? 14 : 12) * 256);
        }
        if (totalCol > 0) sheet.setColumnWidth(totalCol, 12 * 256);
    }

    private MonthSheetBuilt buildMonthSheet(List<OrderItemDetailDTO> items) {
        List<String> products = (items == null ? List.<OrderItemDetailDTO>of() : items).stream()
                .map(OrderItemDetailDTO::getProductName)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<String, String> productUnits = new HashMap<>();
        if (items != null) {
            for (OrderItemDetailDTO item : items) {
                if (item.getProductName() != null && item.getUnit() != null && !item.getUnit().isBlank()) {
                    productUnits.putIfAbsent(item.getProductName(), item.getUnit());
                }
            }
        }

        Map<String, Map<String, CellAgg>> byDateProduct = new TreeMap<>();
        if (items != null) {
            for (OrderItemDetailDTO item : items) {
                if (item.getDate() == null || item.getProductName() == null || item.getProductName().isBlank()) continue;
                String date = item.getDate().toString();
                byDateProduct.computeIfAbsent(date, k -> new HashMap<>())
                        .computeIfAbsent(item.getProductName(), k -> new CellAgg())
                        .add(item.getQuantity(), item.getAmount());
            }
        }

        List<String> dataDates = byDateProduct.entrySet().stream()
                .filter(e -> e.getValue().values().stream().anyMatch(c -> c.qty.compareTo(BigDecimal.ZERO) > 0 || c.amount.compareTo(BigDecimal.ZERO) > 0))
                .map(Map.Entry::getKey)
                .sorted()
                .collect(Collectors.toList());

        List<List<Object>> rows = new ArrayList<>();
        List<Object> row1 = new ArrayList<>();
        row1.add("");
        for (String p : products) row1.addAll(Arrays.asList(p, "", ""));
        row1.add("总计");
        rows.add(row1);

        List<Object> row2 = new ArrayList<>();
        row2.add("日期");
        for (String p : products) {
            String u = productUnits.getOrDefault(p, "");
            row2.add(qtyLabel(u));
            row2.add(priceLabel(u));
            row2.add("金额(元)");
        }
        row2.add("金额(元)");
        rows.add(row2);

        Map<String, CellAgg> productTotals = new LinkedHashMap<>();
        products.forEach(p -> productTotals.put(p, new CellAgg()));
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (String dateStr : dataDates) {
            List<Object> row = new ArrayList<>();
            row.add(dateStr);
            BigDecimal dayTotal = BigDecimal.ZERO;
            for (String p : products) {
                CellAgg cell = byDateProduct.getOrDefault(dateStr, Map.of()).getOrDefault(p, new CellAgg());
                BigDecimal qty = cell.qty;
                BigDecimal amount = cell.amount;
                BigDecimal price = qty.compareTo(BigDecimal.ZERO) > 0
                        ? amount.divide(qty, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                row.add(qty.compareTo(BigDecimal.ZERO) > 0 ? qty.setScale(1, RoundingMode.HALF_UP).doubleValue() : "");
                row.add(price.compareTo(BigDecimal.ZERO) > 0 ? price.setScale(2, RoundingMode.HALF_UP).doubleValue() : "");
                row.add(amount.compareTo(BigDecimal.ZERO) > 0 ? amount.setScale(2, RoundingMode.HALF_UP).doubleValue() : "");
                productTotals.get(p).add(qty, amount);
                dayTotal = dayTotal.add(amount);
            }
            row.add(dayTotal.compareTo(BigDecimal.ZERO) > 0 ? dayTotal.setScale(2, RoundingMode.HALF_UP).doubleValue() : "");
            grandTotal = grandTotal.add(dayTotal);
            rows.add(row);
        }

        List<Object> totalRow = new ArrayList<>();
        totalRow.add("总计");
        for (String p : products) {
            CellAgg t = productTotals.get(p);
            BigDecimal price = t.qty.compareTo(BigDecimal.ZERO) > 0
                    ? t.amount.divide(t.qty, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            totalRow.add(t.qty.compareTo(BigDecimal.ZERO) > 0 ? t.qty.setScale(1, RoundingMode.HALF_UP).doubleValue() : "");
            totalRow.add(price.compareTo(BigDecimal.ZERO) > 0 ? price.setScale(2, RoundingMode.HALF_UP).doubleValue() : "");
            totalRow.add(t.amount.compareTo(BigDecimal.ZERO) > 0 ? t.amount.setScale(2, RoundingMode.HALF_UP).doubleValue() : "");
        }
        totalRow.add(grandTotal.setScale(2, RoundingMode.HALF_UP).doubleValue());
        rows.add(totalRow);

        MonthSheetBuilt built = new MonthSheetBuilt();
        built.setRows(rows);
        built.setProductCount(products.size());
        built.setMaxCol(row2.size() - 1);
        built.setMonthTotal(grandTotal);
        built.setHasData(!dataDates.isEmpty());
        return built;
    }

    private static String qtyLabel(String unit) {
        return unit != null && !unit.isBlank() ? "重量(" + unit + ")" : "重量";
    }

    private static String priceLabel(String unit) {
        return unit != null && !unit.isBlank() ? "单价(元/" + unit + ")" : "单价(元)";
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static double money(BigDecimal v) {
        return nvl(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static CellStyle centerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static void createCell(Row row, int col, String val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val);
        cell.setCellStyle(style);
    }

    private static void createCell(Row row, int col, long val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val);
        cell.setCellStyle(style);
    }

    private static void createCell(Row row, int col, double val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val);
        cell.setCellStyle(style);
    }

    @FunctionalInterface
    private interface MonthItemsLoader {
        List<OrderItemDetailDTO> load(int month);
    }

    private static class CellAgg {
        BigDecimal qty = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;

        void add(BigDecimal q, BigDecimal a) {
            qty = qty.add(nvl(q));
            amount = amount.add(nvl(a));
        }
    }

    @lombok.Data
    private static class MonthSheetBuilt {
        private List<List<Object>> rows = List.of();
        private int productCount;
        private int maxCol;
        private BigDecimal monthTotal = BigDecimal.ZERO;
        private boolean hasData;
        private int month;
    }
}
