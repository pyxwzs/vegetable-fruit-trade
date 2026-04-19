package com.trade.service;

import com.trade.dto.InventoryMovementDTO;
import com.trade.dto.PurchaseOrderDTO;
import com.trade.dto.PurchaseOrderItemDTO;
import com.trade.dto.PurchaseReturnDTO;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final InventoryService inventoryService;

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderDTO orderDTO) {
        Supplier supplier = supplierRepository.findById(orderDTO.getSupplierId())
                .orElseThrow(() -> new BusinessException("供应商不存在"));

        User purchaser = userService.getCurrentUser();

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(generateOrderNo());
        order.setSupplier(supplier);
        order.setPurchaser(purchaser);
        order.setOrderDate(orderDTO.getOrderDate() != null ? orderDTO.getOrderDate() : LocalDate.now());
        order.setExpectedDeliveryDate(orderDTO.getExpectedDeliveryDate());
        order.setPaymentMethod(orderDTO.getPaymentMethod());
        order.setRemark(orderDTO.getRemark());

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseOrderItemDTO itemDTO : orderDTO.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new BusinessException("商品不存在: " + itemDTO.getProductId()));

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setProduct(product);
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(itemDTO.getPrice());

            BigDecimal amount = itemDTO.getPrice().multiply(itemDTO.getQuantity());
            item.setAmount(amount);
            item.setRemark(itemDTO.getRemark());
            item.setReturnedQuantity(BigDecimal.ZERO);

            item.setPurchaseOrder(order);
            order.getItems().add(item);

            totalAmount = totalAmount.add(amount);
        }

        order.setTotalAmount(totalAmount);
        // 已付金额、支付状态仅用实体默认值；资金变动仅能通过 recordPayment（需 purchase:pay）

        return purchaseOrderRepository.save(order);
    }

    private String generateOrderNo() {
        return "PO" + System.currentTimeMillis();
    }

    @Transactional
    public PurchaseOrder approveOrder(Long id) {
        PurchaseOrder order = getPurchaseOrder(id);

        if (order.getStatus() != PurchaseOrder.OrderStatus.PENDING) {
            throw new BusinessException("订单状态不正确");
        }

        order.setStatus(PurchaseOrder.OrderStatus.APPROVED);
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder receiveOrder(Long id) {
        PurchaseOrder order = getPurchaseOrder(id);

        if (order.getStatus() != PurchaseOrder.OrderStatus.SHIPPED) {
            throw new BusinessException("订单状态不正确");
        }

        order.setStatus(PurchaseOrder.OrderStatus.COMPLETED);
        order.setDeliveryDate(LocalDateTime.now().toLocalDate());

        // 更新库存
        for (PurchaseOrderItem item : order.getItems()) {
            InventoryMovementDTO movementDTO = new InventoryMovementDTO();
            movementDTO.setProductId(item.getProduct().getId());
            movementDTO.setWarehouseId(1L); // 默认仓库
            movementDTO.setBatchNo(order.getOrderNo());
            movementDTO.setQuantity(item.getQuantity());
            movementDTO.setPrice(item.getPrice());
            movementDTO.setProductionDate(LocalDateTime.now().toLocalDate());

            inventoryService.addStock(movementDTO);
        }

        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder shipOrder(Long id) {
        PurchaseOrder order = getPurchaseOrder(id);
        if (order.getStatus() != PurchaseOrder.OrderStatus.APPROVED) {
            throw new BusinessException("仅已审核的订单可发货");
        }
        order.setStatus(PurchaseOrder.OrderStatus.SHIPPED);
        return purchaseOrderRepository.save(order);
    }

    @Transactional
    public PurchaseOrder cancelOrder(Long id) {
        PurchaseOrder order = getPurchaseOrder(id);
        if (order.getStatus() != PurchaseOrder.OrderStatus.PENDING) {
            throw new BusinessException("仅待审核的订单可取消");
        }
        order.setStatus(PurchaseOrder.OrderStatus.CANCELLED);
        return purchaseOrderRepository.save(order);
    }

    /**
     * 采购退货：按原入库批次从指定仓库扣减库存，累计已退货数量。
     */
    @Transactional
    public PurchaseOrder purchaseReturn(Long orderId, PurchaseReturnDTO dto) {
        PurchaseOrder order = getPurchaseOrder(orderId);
        if (order.getStatus() != PurchaseOrder.OrderStatus.RECEIVED
                && order.getStatus() != PurchaseOrder.OrderStatus.COMPLETED) {
            throw new BusinessException("仅已收货或已完成的订单可退货");
        }
        long warehouseId = dto.getWarehouseId() != null ? dto.getWarehouseId() : 1L;
        String batchNo = order.getOrderNo();

        for (PurchaseReturnDTO.PurchaseReturnLineDTO line : dto.getLines()) {
            PurchaseOrderItem item = order.getItems().stream()
                    .filter(i -> i.getProduct().getId().equals(line.getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("订单中无该商品"));

            BigDecimal ret = item.getReturnedQuantity() != null ? item.getReturnedQuantity() : BigDecimal.ZERO;
            BigDecimal canReturn = item.getQuantity().subtract(ret);
            if (line.getQuantity().compareTo(canReturn) > 0) {
                throw new BusinessException("退货数量不能超过可退数量（可退 " + canReturn + "）");
            }

            InventoryMovementDTO movementDTO = new InventoryMovementDTO();
            movementDTO.setProductId(line.getProductId());
            movementDTO.setWarehouseId(warehouseId);
            movementDTO.setBatchNo(batchNo);
            movementDTO.setQuantity(line.getQuantity());
            inventoryService.removeStock(movementDTO);

            item.setReturnedQuantity(ret.add(line.getQuantity()));
        }

        return purchaseOrderRepository.save(order);
    }

    public PurchaseOrder getPurchaseOrder(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("采购订单不存在"));
    }

    public Page<PurchaseOrder> getPurchaseOrders(String keyword, PurchaseOrder.OrderStatus status, Pageable pageable) {
        Specification<PurchaseOrder> spec = (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
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
            Long scope = userService.resolveBizDataScopeUserId();
            if (scope != null) {
                ps.add(cb.equal(root.get("purchaser").get("id"), scope));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return purchaseOrderRepository.findAll(spec, pageable);
    }

    /**
     * 登记采购付款：订单「已付金额 / 支付状态」的唯一变更入口（审核、发货、收货、退货等均不修改资金字段）。
     */
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
        BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
        paid = paid.add(amount);
        if (paid.compareTo(total) > 0) {
            throw new BusinessException("累计付款不能超过订单总额（总额 ¥" + total + "）");
        }
        order.setPaidAmount(paid);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            order.setPaymentStatus(PurchaseOrder.PaymentStatus.PAID);
        } else if (paid.compareTo(total) >= 0) {
            order.setPaymentStatus(PurchaseOrder.PaymentStatus.PAID);
        } else {
            order.setPaymentStatus(PurchaseOrder.PaymentStatus.PARTIAL);
        }
        return purchaseOrderRepository.save(order);
    }
}