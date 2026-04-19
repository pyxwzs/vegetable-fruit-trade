package com.trade.service;

import com.trade.dto.SalesOrderDTO;
import com.trade.dto.SalesOrderItemDTO;
import com.trade.dto.SalesReturnDTO;
import com.trade.dto.InventoryMovementDTO;
import com.trade.entity.*;
import com.trade.exception.BusinessException;
import com.trade.repository.CustomerRepository;
import com.trade.repository.ProductRepository;
import com.trade.repository.SalesOrderRepository;
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
public class SalesService {
    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final UserService userService;
    private final InventoryService inventoryService;

    @Transactional
    public SalesOrder createSalesOrder(SalesOrderDTO orderDTO) {
        Customer customer = customerRepository.findById(orderDTO.getCustomerId())
                .orElseThrow(() -> new BusinessException("客户不存在"));

        if (customer.getStatus() != Customer.CustomerStatus.ACTIVE) {
            throw new BusinessException("客户已停用或冻结，无法下单");
        }

        if (customer.getCreditLevel() == Customer.CreditLevel.D) {
            throw new BusinessException("客户信用等级不足，无法下单");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SalesOrderItemDTO itemDTO : orderDTO.getItems()) {
            productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new BusinessException("商品不存在: " + itemDTO.getProductId()));
            BigDecimal amount = itemDTO.getPrice().multiply(itemDTO.getQuantity());
            totalAmount = totalAmount.add(amount);
        }

        BigDecimal creditLimit = customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO;
        if (creditLimit.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal exposure = salesOrderRepository.sumUnpaidExposure(customer.getId());
            if (exposure.add(totalAmount).compareTo(creditLimit) > 0) {
                throw new BusinessException("超出客户授信额度（未结清占用 + 本单金额 > 额度）");
            }
        }

        User salesman = userService.getCurrentUser();

        SalesOrder order = new SalesOrder();
        order.setOrderNo(generateOrderNo());
        order.setCustomer(customer);
        order.setSalesman(salesman);
        order.setOrderDate(orderDTO.getOrderDate() != null ? orderDTO.getOrderDate() : LocalDate.now());
        order.setDeliveryDate(orderDTO.getDeliveryDate());
        order.setPaymentMethod(orderDTO.getPaymentMethod());
        order.setRemark(orderDTO.getRemark());
        order.setReceivedAmount(BigDecimal.ZERO);
        order.setPaymentStatus(SalesOrder.PaymentStatus.UNPAID);
        order.setStatus(SalesOrder.OrderStatus.PENDING);

        for (SalesOrderItemDTO itemDTO : orderDTO.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId()).get();

            SalesOrderItem item = new SalesOrderItem();
            item.setProduct(product);
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(itemDTO.getPrice());
            item.setReturnedQuantity(BigDecimal.ZERO);

            BigDecimal amount = itemDTO.getPrice().multiply(itemDTO.getQuantity());
            item.setAmount(amount);
            item.setRemark(itemDTO.getRemark());

            item.setSalesOrder(order);
            order.getItems().add(item);
        }

        order.setTotalAmount(totalAmount);
        // 已收金额、支付状态仅初始化为未付；资金变动仅能通过 recordReceipt（需 sales:collect）

        return salesOrderRepository.save(order);
    }

    private String generateOrderNo() {
        return "SO" + System.currentTimeMillis();
    }

    @Transactional
    public SalesOrder approveOrder(Long id) {
        SalesOrder order = getSalesOrder(id);

        if (order.getStatus() != SalesOrder.OrderStatus.PENDING) {
            throw new BusinessException("订单状态不正确");
        }

        order.setStatus(SalesOrder.OrderStatus.APPROVED);
        return salesOrderRepository.save(order);
    }

    /**
     * 从指定仓库按 FIFO 扣减库存后标记为已发货。
     */
    @Transactional
    public SalesOrder shipOrder(Long id, Long warehouseId) {
        long wid = warehouseId != null ? warehouseId : 1L;
        SalesOrder order = salesOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("销售订单不存在"));

        if (order.getStatus() != SalesOrder.OrderStatus.APPROVED) {
            throw new BusinessException("仅已审核的订单可发货");
        }

        for (SalesOrderItem item : order.getItems()) {
            inventoryService.deductStockFifo(item.getProduct().getId(), wid, item.getQuantity());
        }

        order.setStatus(SalesOrder.OrderStatus.COMPLETED);
        return salesOrderRepository.save(order);
    }

    @Transactional
    public SalesOrder cancelOrder(Long id) {
        SalesOrder order = getSalesOrder(id);
        if (order.getStatus() != SalesOrder.OrderStatus.PENDING) {
            throw new BusinessException("仅待审核的订单可取消");
        }
        order.setStatus(SalesOrder.OrderStatus.CANCELLED);
        return salesOrderRepository.save(order);
    }

    /**
     * 销售退货：按统一批次回补库存，累计已退货数量。
     */
    @Transactional
    public SalesOrder salesReturn(Long orderId, SalesReturnDTO dto) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("销售订单不存在"));

        if (order.getStatus() != SalesOrder.OrderStatus.SHIPPED
                && order.getStatus() != SalesOrder.OrderStatus.DELIVERED
                && order.getStatus() != SalesOrder.OrderStatus.COMPLETED) {
            throw new BusinessException("仅已发货、已送达或已完成的订单可退货");
        }

        long warehouseId = dto.getWarehouseId() != null ? dto.getWarehouseId() : 1L;
        String batchNo = order.getOrderNo() + "-SR";

        for (SalesReturnDTO.SalesReturnLineDTO line : dto.getLines()) {
            SalesOrderItem item = order.getItems().stream()
                    .filter(i -> i.getProduct().getId().equals(line.getProductId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException("订单中无该商品"));

            BigDecimal ret = item.getReturnedQuantity() != null ? item.getReturnedQuantity() : BigDecimal.ZERO;
            BigDecimal canReturn = item.getQuantity().subtract(ret);
            if (line.getQuantity().compareTo(canReturn) > 0) {
                throw new BusinessException("退货数量不能超过可退数量（可退 " + canReturn + "）");
            }

            InventoryMovementDTO in = new InventoryMovementDTO();
            in.setProductId(line.getProductId());
            in.setWarehouseId(warehouseId);
            in.setBatchNo(batchNo);
            in.setQuantity(line.getQuantity());
            in.setPrice(item.getPrice());
            in.setProductionDate(LocalDate.now());
            inventoryService.addStock(in);

            item.setReturnedQuantity(ret.add(line.getQuantity()));
        }

        return salesOrderRepository.save(order);
    }

    public SalesOrder getSalesOrder(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("销售订单不存在"));
    }

    public Page<SalesOrder> getSalesOrders(String keyword, SalesOrder.OrderStatus status, Pageable pageable) {
        Specification<SalesOrder> spec = (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            List<Predicate> ps = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.trim() + "%";
                ps.add(cb.or(
                        cb.like(root.get("orderNo"), kw),
                        cb.like(root.get("customer").get("name"), kw)
                ));
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            Long scope = userService.resolveBizDataScopeUserId();
            if (scope != null) {
                ps.add(cb.equal(root.get("salesman").get("id"), scope));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return salesOrderRepository.findAll(spec, pageable);
    }

    /**
     * 登记销售收款：订单「已收金额 / 支付状态」的唯一变更入口（审核、发货、退货等均不修改资金字段）。
     */
    @Transactional
    public SalesOrder recordReceipt(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("收款金额必须大于 0");
        }
        SalesOrder order = getSalesOrder(id);
        if (order.getStatus() == SalesOrder.OrderStatus.CANCELLED) {
            throw new BusinessException("已取消的订单不可收款");
        }
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal received = order.getReceivedAmount() != null ? order.getReceivedAmount() : BigDecimal.ZERO;
        received = received.add(amount);
        if (received.compareTo(total) > 0) {
            throw new BusinessException("累计收款不能超过订单总额（总额 ¥" + total + "）");
        }
        order.setReceivedAmount(received);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            order.setPaymentStatus(SalesOrder.PaymentStatus.PAID);
        } else if (received.compareTo(total) >= 0) {
            order.setPaymentStatus(SalesOrder.PaymentStatus.PAID);
        } else {
            order.setPaymentStatus(SalesOrder.PaymentStatus.PARTIAL);
        }
        return salesOrderRepository.save(order);
    }
}
