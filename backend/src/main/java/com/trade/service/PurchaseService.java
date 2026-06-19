package com.trade.service;

import com.trade.dto.InventoryMovementDTO;
import com.trade.dto.PurchaseOrderDTO;
import com.trade.dto.PurchaseOrderItemDTO;
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

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderDTO dto) {
        Supplier supplier = supplierRepository.findById(dto.getSupplierId())
                .orElseThrow(() -> new BusinessException("供应商不存在"));

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo("PO" + System.currentTimeMillis());
        order.setSupplier(supplier);
        order.setOrderDate(dto.getOrderDate() != null ? dto.getOrderDate() : LocalDate.now());
        order.setPaymentMethod(dto.getPaymentMethod());
        order.setRemark(dto.getRemark());

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
            item.setRemark(itemDTO.getRemark());
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
    public PurchaseOrder recordPayment(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("付款金额必须大于 0");
        }
        PurchaseOrder order = getPurchaseOrder(id);
        if (order.getStatus() == PurchaseOrder.OrderStatus.CANCELLED) {
            throw new BusinessException("已取消的订单不可付款");
        }
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal paid = (order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO).add(amount);
        if (paid.compareTo(total) > 0) {
            throw new BusinessException("累计付款不能超过订单总额（总额 ¥" + total + "）");
        }
        order.setPaidAmount(paid);
        order.setPaymentStatus(paid.compareTo(total) >= 0 ? PurchaseOrder.PaymentStatus.PAID : PurchaseOrder.PaymentStatus.PARTIAL);
        return purchaseOrderRepository.save(order);
    }

    public PurchaseOrder getPurchaseOrder(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("采购订单不存在"));
    }

    public Page<PurchaseOrder> getPurchaseOrders(String keyword, PurchaseOrder.OrderStatus status, Pageable pageable) {
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
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return purchaseOrderRepository.findAll(spec, pageable);
    }
}
