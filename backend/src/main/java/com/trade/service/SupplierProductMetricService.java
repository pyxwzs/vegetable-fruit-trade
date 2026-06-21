package com.trade.service;

import com.trade.dto.SupplierMetricCompletionGroupDTO;
import com.trade.dto.SupplierMetricCompletionReportDTO;
import com.trade.dto.SupplierProductMetricDTO;
import com.trade.entity.Product;
import com.trade.entity.Supplier;
import com.trade.entity.SupplierProductMetric;
import com.trade.exception.BusinessException;
import com.trade.repository.ProductRepository;
import com.trade.repository.PurchaseOrderItemRepository;
import com.trade.repository.SupplierProductMetricRepository;
import com.trade.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupplierProductMetricService {

    private final SupplierProductMetricRepository metricRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    public Page<SupplierProductMetricDTO> page(String keyword, Long supplierId, Long productId, String status,
                                               String periodType, Integer year, Integer month,
                                               Pageable pageable) {
        PeriodContext ctx = resolvePeriodContext(periodType, year, month);
        Specification<SupplierProductMetric> spec = buildSpec(keyword, supplierId, productId, status, ctx);
        Map<String, BigDecimal> actualMap = loadActualQtyMap(ctx);
        return metricRepository.findAll(spec, pageable).map(m -> toDto(m, actualMap));
    }

    public SupplierMetricCompletionReportDTO completionReport(String periodType, Integer year, Integer month,
                                                                Long supplierId) {
        PeriodContext ctx = resolvePeriodContext(periodType, year, month);
        Specification<SupplierProductMetric> spec = buildSpec(null, supplierId, null, "ACTIVE", ctx);
        List<SupplierProductMetric> metrics = metricRepository.findAll(spec);
        Map<String, BigDecimal> actualMap = loadActualQtyMap(ctx);

        Map<Long, SupplierMetricCompletionGroupDTO> groupMap = new LinkedHashMap<>();
        SupplierMetricCompletionReportDTO report = new SupplierMetricCompletionReportDTO();
        report.setPeriodType(ctx.periodType.name());
        report.setYear(ctx.year);
        report.setMonth(ctx.periodType == SupplierProductMetric.PeriodType.MONTH ? ctx.month : null);
        report.setPeriodLabel(formatPeriodLabel(ctx));

        for (SupplierProductMetric m : metrics) {
            SupplierProductMetricDTO item = toDto(m, actualMap);
            SupplierMetricCompletionGroupDTO group = groupMap.computeIfAbsent(
                    m.getSupplier().getId(),
                    id -> {
                        SupplierMetricCompletionGroupDTO g = new SupplierMetricCompletionGroupDTO();
                        g.setSupplierId(id);
                        g.setSupplierName(m.getSupplier().getName());
                        return g;
                    });
            group.getItems().add(item);
            accumulate(group, item);
        }

        List<SupplierMetricCompletionGroupDTO> groups = new ArrayList<>(groupMap.values());
        groups.sort(Comparator.comparing(SupplierMetricCompletionGroupDTO::getSupplierName));
        for (SupplierMetricCompletionGroupDTO g : groups) {
            g.getItems().sort(Comparator.comparing(SupplierProductMetricDTO::getProductName));
            finalizeTotals(g);
        }
        report.setGroups(groups);

        BigDecimal totalTarget = BigDecimal.ZERO;
        BigDecimal totalActual = BigDecimal.ZERO;
        for (SupplierMetricCompletionGroupDTO g : groups) {
            totalTarget = totalTarget.add(g.getTotalTarget());
            totalActual = totalActual.add(g.getTotalActual());
        }
        report.setTotalTarget(totalTarget);
        report.setTotalActual(totalActual);
        applyGapFields(report, totalActual, totalTarget);
        report.setCompletionPercent(calcCompletionPercent(totalActual, totalTarget));
        return report;
    }

    public SupplierProductMetricDTO getById(Long id) {
        SupplierProductMetric m = getEntity(id);
        PeriodContext ctx = periodContextOf(m);
        return toDto(m, loadActualQtyMap(ctx));
    }

    @Transactional
    public SupplierProductMetricDTO create(SupplierProductMetricDTO dto) {
        validateTargetQty(dto.getTargetQty());
        PeriodContext ctx = resolvePeriodContext(dto.getPeriodType(), dto.getYear(), dto.getMonth());
        if (metricRepository.existsBySupplier_IdAndProduct_IdAndPeriodTypeAndYearAndMonth(
                dto.getSupplierId(), dto.getProductId(), ctx.periodType, ctx.year, ctx.month)) {
            throw new BusinessException("该供应商在此周期已有此商品的订量指标");
        }
        SupplierProductMetric m = new SupplierProductMetric();
        fillEntity(m, dto, ctx);
        return toDto(metricRepository.save(m), loadActualQtyMap(ctx));
    }

    @Transactional
    public SupplierProductMetricDTO update(Long id, SupplierProductMetricDTO dto) {
        validateTargetQty(dto.getTargetQty());
        SupplierProductMetric m = getEntity(id);
        PeriodContext ctx = resolvePeriodContext(dto.getPeriodType(), dto.getYear(), dto.getMonth());
        if (metricRepository.existsBySupplier_IdAndProduct_IdAndPeriodTypeAndYearAndMonthAndIdNot(
                dto.getSupplierId(), dto.getProductId(), ctx.periodType, ctx.year, ctx.month, id)) {
            throw new BusinessException("该供应商在此周期已有此商品的订量指标");
        }
        fillEntity(m, dto, ctx);
        return toDto(metricRepository.save(m), loadActualQtyMap(ctx));
    }

    @Transactional
    public void delete(Long id) {
        if (!metricRepository.existsById(id)) {
            throw new BusinessException("指标不存在");
        }
        metricRepository.deleteById(id);
    }

    private Specification<SupplierProductMetric> buildSpec(String keyword, Long supplierId, Long productId,
                                                           String status, PeriodContext ctx) {
        return (root, query, cb) -> {
            if (query != null) query.distinct(true);
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("periodType"), ctx.periodType));
            ps.add(cb.equal(root.get("year"), ctx.year));
            ps.add(cb.equal(root.get("month"), ctx.month));
            if (supplierId != null) {
                ps.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            if (productId != null) {
                ps.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (status != null && !status.isBlank()) {
                ps.add(cb.equal(root.get("status"),
                        SupplierProductMetric.MetricStatus.valueOf(status.trim().toUpperCase())));
            }
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim() + "%";
                Join<Object, Object> supplier = root.join("supplier");
                Join<Object, Object> product = root.join("product");
                ps.add(cb.or(
                        cb.like(supplier.get("name"), kw),
                        cb.like(product.get("name"), kw),
                        cb.like(root.get("remark"), kw)
                ));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
    }

    private SupplierProductMetric getEntity(Long id) {
        return metricRepository.findById(id)
                .orElseThrow(() -> new BusinessException("指标不存在"));
    }

    private void fillEntity(SupplierProductMetric m, SupplierProductMetricDTO dto, PeriodContext ctx) {
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new BusinessException("供应商不存在"));
        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new BusinessException("商品不存在"));
        m.setSupplier(supplier);
        m.setProduct(product);
        m.setPeriodType(ctx.periodType);
        m.setYear(ctx.year);
        m.setMonth(ctx.month);
        m.setTargetQty(dto.getTargetQty());
        m.setRemark(emptyToNull(dto.getRemark()));
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            m.setStatus(SupplierProductMetric.MetricStatus.valueOf(dto.getStatus().trim().toUpperCase()));
        } else if (m.getId() == null) {
            m.setStatus(SupplierProductMetric.MetricStatus.ACTIVE);
        }
    }

    private Map<String, BigDecimal> loadActualQtyMap(PeriodContext ctx) {
        List<Object[]> rows = ctx.periodType == SupplierProductMetric.PeriodType.YEAR
                ? purchaseOrderItemRepository.sumQuantityBySupplierProductYear(ctx.year)
                : purchaseOrderItemRepository.sumQuantityBySupplierProductMonth(ctx.year, ctx.month);
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] r : rows) {
            Long sid = r[0] instanceof Number ? ((Number) r[0]).longValue() : null;
            Long pid = r[1] instanceof Number ? ((Number) r[1]).longValue() : null;
            BigDecimal qty = toBigDecimal(r[r.length - 1]);
            if (sid != null && pid != null) {
                map.put(sid + "-" + pid, qty);
            }
        }
        return map;
    }

    private SupplierProductMetricDTO toDto(SupplierProductMetric m, Map<String, BigDecimal> actualMap) {
        SupplierProductMetricDTO dto = new SupplierProductMetricDTO();
        dto.setId(m.getId());
        dto.setSupplierId(m.getSupplier().getId());
        dto.setSupplierName(m.getSupplier().getName());
        dto.setProductId(m.getProduct().getId());
        dto.setProductName(m.getProduct().getName());
        dto.setProductUnit(m.getProduct().getUnit());
        dto.setPeriodType(m.getPeriodType().name());
        dto.setYear(m.getYear());
        dto.setMonth(m.getMonth());
        dto.setPeriodLabel(formatPeriodLabel(periodContextOf(m)));
        dto.setTargetQty(m.getTargetQty());

        String key = m.getSupplier().getId() + "-" + m.getProduct().getId();
        BigDecimal actual = actualMap.getOrDefault(key, BigDecimal.ZERO);
        BigDecimal target = m.getTargetQty() != null ? m.getTargetQty() : BigDecimal.ZERO;
        dto.setActualQty(actual);
        applyGapFields(dto, actual, target);
        dto.setCompletionPercent(calcCompletionPercent(actual, target));

        dto.setRemark(m.getRemark());
        dto.setStatus(m.getStatus() != null ? m.getStatus().name() : null);
        dto.setCreateTime(m.getCreateTime());
        dto.setUpdateTime(m.getUpdateTime());
        return dto;
    }

    private static void accumulate(SupplierMetricCompletionGroupDTO group, SupplierProductMetricDTO item) {
        group.setTotalTarget(group.getTotalTarget().add(safe(item.getTargetQty())));
        group.setTotalActual(group.getTotalActual().add(safe(item.getActualQty())));
    }

    private static void finalizeTotals(SupplierMetricCompletionGroupDTO group) {
        applyGapFields(group, group.getTotalActual(), group.getTotalTarget());
        group.setCompletionPercent(calcCompletionPercent(group.getTotalActual(), group.getTotalTarget()));
    }

    private static void applyGapFields(SupplierProductMetricDTO dto, BigDecimal actual, BigDecimal target) {
        BigDecimal gap = actual.subtract(target);
        dto.setGapQty(gap);
        dto.setExcessQty(gap.compareTo(BigDecimal.ZERO) > 0 ? gap : BigDecimal.ZERO);
        dto.setShortfallQty(gap.compareTo(BigDecimal.ZERO) < 0 ? gap.negate() : BigDecimal.ZERO);
    }

    private static void applyGapFields(SupplierMetricCompletionGroupDTO dto, BigDecimal actual, BigDecimal target) {
        BigDecimal gap = actual.subtract(target);
        dto.setTotalGap(gap);
        dto.setTotalExcess(gap.compareTo(BigDecimal.ZERO) > 0 ? gap : BigDecimal.ZERO);
        dto.setTotalShortfall(gap.compareTo(BigDecimal.ZERO) < 0 ? gap.negate() : BigDecimal.ZERO);
    }

    private static void applyGapFields(SupplierMetricCompletionReportDTO dto, BigDecimal actual, BigDecimal target) {
        BigDecimal gap = actual.subtract(target);
        dto.setTotalGap(gap);
        dto.setTotalExcess(gap.compareTo(BigDecimal.ZERO) > 0 ? gap : BigDecimal.ZERO);
        dto.setTotalShortfall(gap.compareTo(BigDecimal.ZERO) < 0 ? gap.negate() : BigDecimal.ZERO);
    }

    private static int calcCompletionPercent(BigDecimal actual, BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) return 0;
        return actual.multiply(BigDecimal.valueOf(100))
                .divide(target, 0, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(999))
                .intValue();
    }

    private static PeriodContext resolvePeriodContext(String periodType, Integer year, Integer month) {
        SupplierProductMetric.PeriodType pt = parsePeriodType(periodType);
        int y = year != null ? year : LocalDate.now().getYear();
        if (pt == SupplierProductMetric.PeriodType.YEAR) {
            return new PeriodContext(pt, y, 0);
        }
        int m = month != null ? month : LocalDate.now().getMonthValue();
        if (m < 1 || m > 12) {
            throw new BusinessException("月份为 1-12");
        }
        return new PeriodContext(pt, y, m);
    }

    private static PeriodContext periodContextOf(SupplierProductMetric m) {
        return new PeriodContext(m.getPeriodType(), m.getYear(), m.getMonth());
    }

    private static SupplierProductMetric.PeriodType parsePeriodType(String periodType) {
        if (periodType == null || periodType.isBlank()) {
            return SupplierProductMetric.PeriodType.MONTH;
        }
        try {
            return SupplierProductMetric.PeriodType.valueOf(periodType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("指标类型无效");
        }
    }

    private static String formatPeriodLabel(PeriodContext ctx) {
        if (ctx.periodType == SupplierProductMetric.PeriodType.YEAR) {
            return ctx.year + "年";
        }
        return ctx.year + "年" + ctx.month + "月";
    }

    private static void validateTargetQty(BigDecimal targetQty) {
        if (targetQty == null || targetQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("订量指标须大于 0");
        }
    }

    private static BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
        return new BigDecimal(v.toString());
    }

    private static String emptyToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }

    private static final class PeriodContext {
        private final SupplierProductMetric.PeriodType periodType;
        private final int year;
        private final int month;

        private PeriodContext(SupplierProductMetric.PeriodType periodType, int year, int month) {
            this.periodType = periodType;
            this.year = year;
            this.month = month;
        }
    }
}
