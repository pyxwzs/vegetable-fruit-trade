package com.trade.service;

import com.trade.dto.*;
import com.trade.entity.*;
import com.trade.exception.BusinessException;
import com.trade.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final PurchasePaymentRepository purchasePaymentRepository;

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderDTO dto) {
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new BusinessException("供应商不存在"));

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo("PO" + System.currentTimeMillis());
        order.setSupplier(supplier);
        order.setOrderDate(dto.getOrderDate() != null ? dto.getOrderDate() : LocalDate.now());
        order.setPaymentMethod(dto.getPaymentMethod());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (PurchaseOrderItemDTO itemDTO : dto.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new BusinessException("商品不存在: " + itemDTO.getProductId()));
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(product);
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(itemDTO.getPrice());
            BigDecimal amount = itemDTO.getPrice().multiply(itemDTO.getQuantity());
            item.setAmount(amount);
            item.setPurchaseOrder(order);
            order.getItems().add(item);
            totalAmount = totalAmount.add(amount);
        }
        order.setTotalAmount(totalAmount);
        return purchaseOrderRepository.save(order);
    }

    /** 完成采购：入库并标记完成 */
    @Transactional
    public PurchaseOrder completeOrder(Long id, Long warehouseId) {
        PurchaseOrder order = getPurchaseOrder(id);
        if (order.getStatus() != PurchaseOrder.OrderStatus.PENDING) {
            throw new BusinessException("仅待处理的订单可完成");
        }
        long wid = warehouseId != null ? warehouseId : 1L;
        for (PurchaseOrderItem item : order.getItems()) {
            InventoryMovementDTO mv = new InventoryMovementDTO();
            mv.setProductId(item.getProduct().getId());
            mv.setWarehouseId(wid);
            mv.setQuantity(item.getQuantity());
            inventoryService.addStock(mv);
        }
        order.setStatus(PurchaseOrder.OrderStatus.COMPLETED);
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder cancelOrder(Long id) {
        PurchaseOrder order = getPurchaseOrder(id);
        if (order.getStatus() != PurchaseOrder.OrderStatus.PENDING) {
            throw new BusinessException("仅待处理的订单可取消");
        }
        order.setStatus(PurchaseOrder.OrderStatus.CANCELLED);
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder recordPayment(Long id, AddPaymentDTO dto) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("付款金额必须大于 0");
        }
        PurchaseOrder order = getPurchaseOrder(id);
        if (order.getStatus() == PurchaseOrder.OrderStatus.CANCELLED) {
            throw new BusinessException("已取消的订单不可付款");
        }
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal newPaid = (order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO).add(dto.getAmount());
        if (newPaid.compareTo(total) > 0) {
            throw new BusinessException("累计付款不能超过订单总额（总额 ¥" + total + "）");
        }

        PurchasePayment record = new PurchasePayment();
        record.setOrder(order);
        record.setPaymentDate(dto.getPaymentDate() != null ? dto.getPaymentDate() : LocalDate.now());
        record.setAmount(dto.getAmount());
        record.setPaymentMethod(dto.getPaymentMethod());
        purchasePaymentRepository.save(record);

        order.setPaidAmount(newPaid);
        order.setPaymentStatus(newPaid.compareTo(total) >= 0 ? PurchaseOrder.PaymentStatus.PAID : PurchaseOrder.PaymentStatus.PARTIAL);
        return purchaseOrderRepository.save(order);
    }

    public List<PaymentRecordDTO> getPaymentHistory(Long orderId) {
        return purchasePaymentRepository.findByOrderIdOrderByPaymentDateDesc(orderId)
                .stream().map(p -> {
                    PaymentRecordDTO dto = new PaymentRecordDTO();
                    dto.setId(p.getId());
                    dto.setPaymentDate(p.getPaymentDate());
                    dto.setAmount(p.getAmount());
                    dto.setPaymentMethod(p.getPaymentMethod());
                    dto.setCreatedAt(p.getCreatedAt());
                    return dto;
                }).collect(Collectors.toList());
    }

    public java.util.Map<String, Object> getPendingStats() {
        long count = purchaseOrderRepository.countByStatus(PurchaseOrder.OrderStatus.PENDING);
        java.math.BigDecimal total = purchaseOrderRepository.sumPendingAmount();
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("count", count);
        map.put("totalAmount", total != null ? total : java.math.BigDecimal.ZERO);
        return map;
    }

    public PurchaseOrder getPurchaseOrder(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("采购订单不存在"));
    }

    public Page<PurchaseOrder> getPurchaseOrders(String keyword, PurchaseOrder.OrderStatus status,
                                                  String paymentStatusRaw,
                                                  LocalDate startDate, LocalDate endDate,
                                                  Pageable pageable) {
        List<PurchaseOrder.PaymentStatus> paymentStatuses = parsePaymentStatuses(paymentStatusRaw,
                PurchaseOrder.PaymentStatus.class);
        Specification<PurchaseOrder> spec = (root, query, cb) -> {
            if (query != null) query.distinct(true);
            List<Predicate> ps = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim() + "%";
                ps.add(cb.or(
                        cb.like(root.get("orderNo"), kw),
                        cb.like(root.get("supplier").get("name"), kw)
                ));
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (!paymentStatuses.isEmpty()) {
                ps.add(root.get("paymentStatus").in(paymentStatuses));
            }
            if (startDate != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("orderDate"), startDate));
            }
            if (endDate != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("orderDate"), endDate));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return purchaseOrderRepository.findAll(spec, pageable);
    }

    private <E extends Enum<E>> List<E> parsePaymentStatuses(String raw, Class<E> enumClass) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        List<E> result = new ArrayList<>();
        for (String s : raw.split(",")) {
            try { result.add(Enum.valueOf(enumClass, s.trim().toUpperCase())); } catch (Exception ignored) {}
        }
        return result;
    }
}
