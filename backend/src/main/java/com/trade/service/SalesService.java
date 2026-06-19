package com.trade.service;

import com.trade.dto.SalesOrderDTO;
import com.trade.dto.SalesOrderItemDTO;
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
    private final InventoryService inventoryService;

    @Transactional
    public SalesOrder createSalesOrder(SalesOrderDTO dto) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new BusinessException("客户不存在"));
        if (customer.getStatus() != Customer.CustomerStatus.ACTIVE) {
            throw new BusinessException("客户已停用，无法下单");
        }

        SalesOrder order = new SalesOrder();
        order.setOrderNo("SO" + System.currentTimeMillis());
        order.setCustomer(customer);
        order.setOrderDate(dto.getOrderDate() != null ? dto.getOrderDate() : LocalDate.now());
        order.setPaymentMethod(dto.getPaymentMethod());
        order.setRemark(dto.getRemark());

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (SalesOrderItemDTO itemDTO : dto.getItems()) {
            Product product = productRepository.findById(itemDTO.getProductId())
                    .orElseThrow(() -> new BusinessException("商品不存在: " + itemDTO.getProductId()));
            SalesOrderItem item = new SalesOrderItem();
            item.setProduct(product);
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(itemDTO.getPrice());
            BigDecimal amount = itemDTO.getPrice().multiply(itemDTO.getQuantity());
            item.setAmount(amount);
            item.setRemark(itemDTO.getRemark());
            item.setSalesOrder(order);
            order.getItems().add(item);
            totalAmount = totalAmount.add(amount);
        }
        order.setTotalAmount(totalAmount);
        return salesOrderRepository.save(order);
    }

    /** 完成销售：扣减库存并标记完成 */
    @Transactional
    public SalesOrder completeOrder(Long id, Long warehouseId) {
        SalesOrder order = getSalesOrder(id);
        if (order.getStatus() != SalesOrder.OrderStatus.PENDING) {
            throw new BusinessException("仅待处理的订单可完成");
        }
        long wid = warehouseId != null ? warehouseId : 1L;
        for (SalesOrderItem item : order.getItems()) {
            inventoryService.deductStock(item.getProduct().getId(), wid, item.getQuantity());
        }
        order.setStatus(SalesOrder.OrderStatus.COMPLETED);
        return salesOrderRepository.save(order);
    }

    @Transactional
    public SalesOrder cancelOrder(Long id) {
        SalesOrder order = getSalesOrder(id);
        if (order.getStatus() != SalesOrder.OrderStatus.PENDING) {
            throw new BusinessException("仅待处理的订单可取消");
        }
        order.setStatus(SalesOrder.OrderStatus.CANCELLED);
        return salesOrderRepository.save(order);
    }

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
        BigDecimal received = (order.getReceivedAmount() != null ? order.getReceivedAmount() : BigDecimal.ZERO).add(amount);
        if (received.compareTo(total) > 0) {
            throw new BusinessException("累计收款不能超过订单总额（总额 ¥" + total + "）");
        }
        order.setReceivedAmount(received);
        order.setPaymentStatus(received.compareTo(total) >= 0 ? SalesOrder.PaymentStatus.PAID : SalesOrder.PaymentStatus.PARTIAL);
        return salesOrderRepository.save(order);
    }

    public SalesOrder getSalesOrder(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new BusinessException("销售订单不存在"));
    }

    public Page<SalesOrder> getSalesOrders(String keyword, SalesOrder.OrderStatus status, Pageable pageable) {
        Specification<SalesOrder> spec = (root, query, cb) -> {
            if (query != null) query.distinct(true);
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
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return salesOrderRepository.findAll(spec, pageable);
    }
}
