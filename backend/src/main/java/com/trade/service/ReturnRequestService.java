package com.trade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.dto.*;
import com.trade.entity.*;
import com.trade.exception.BusinessException;
import com.trade.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReturnRequestService {

    private final ReturnFinanceRequestRepository returnRepo;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ReturnRequestDTO submitPurchaseReturn(Long orderId, PurchaseReturnDTO dto) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("采购订单不存在"));
        if (order.getStatus() != PurchaseOrder.OrderStatus.COMPLETED) {
            throw new BusinessException("仅已完成的订单可申请退货");
        }

        List<ReturnRequestLineDTO> lines = new ArrayList<>();
        BigDecimal returnAmount = BigDecimal.ZERO;
        for (PurchaseReturnDTO.PurchaseReturnLineDTO line : dto.getLines()) {
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;
            PurchaseOrderItem item = findPurchaseItem(order, line.getProductId());
            lines.add(new ReturnRequestLineDTO(line.getProductId(), item.getProduct().getName(), line.getQuantity(), item.getPrice()));
            returnAmount = returnAmount.add(line.getQuantity().multiply(item.getPrice()));
        }
        if (lines.isEmpty()) throw new BusinessException("退货明细不能为空");

        ReturnFinanceRequest req = new ReturnFinanceRequest();
        req.setKind(ReturnFinanceRequest.Kind.PURCHASE);
        req.setOrderId(orderId);
        req.setOrderNo(order.getOrderNo());
        req.setReturnAmount(returnAmount);
        req.setLinesJson(toJson(lines));
        return toDTO(returnRepo.save(req));
    }

    @Transactional
    public ReturnRequestDTO submitSalesReturn(Long orderId, SalesReturnDTO dto) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("销售订单不存在"));
        if (order.getStatus() != SalesOrder.OrderStatus.COMPLETED) {
            throw new BusinessException("仅已完成的订单可申请退货");
        }

        List<ReturnRequestLineDTO> lines = new ArrayList<>();
        BigDecimal returnAmount = BigDecimal.ZERO;
        for (SalesReturnDTO.SalesReturnLineDTO line : dto.getLines()) {
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;
            SalesOrderItem item = findSalesItem(order, line.getProductId());
            lines.add(new ReturnRequestLineDTO(line.getProductId(), item.getProduct().getName(), line.getQuantity(), item.getPrice()));
            returnAmount = returnAmount.add(line.getQuantity().multiply(item.getPrice()));
        }
        if (lines.isEmpty()) throw new BusinessException("退货明细不能为空");

        ReturnFinanceRequest req = new ReturnFinanceRequest();
        req.setKind(ReturnFinanceRequest.Kind.SALES);
        req.setOrderId(orderId);
        req.setOrderNo(order.getOrderNo());
        req.setReturnAmount(returnAmount);
        req.setLinesJson(toJson(lines));
        return toDTO(returnRepo.save(req));
    }

    @Transactional
    public ReturnRequestDTO approve(Long requestId, Long warehouseId) {
        ReturnFinanceRequest req = getRequest(requestId);
        if (req.getStatus() != ReturnFinanceRequest.Status.PENDING) {
            throw new BusinessException("仅待审批的申请可审批");
        }
        long wid = warehouseId != null ? warehouseId : 1L;
        List<ReturnRequestLineDTO> lines = parseLines(req.getLinesJson());

        if (req.getKind() == ReturnFinanceRequest.Kind.PURCHASE) {
            for (ReturnRequestLineDTO line : lines) {
                InventoryMovementDTO mv = new InventoryMovementDTO();
                mv.setProductId(line.getProductId());
                mv.setWarehouseId(wid);
                mv.setQuantity(line.getQuantity());
                inventoryService.removeStock(mv);
            }
            PurchaseOrder order = purchaseOrderRepository.findById(req.getOrderId()).orElse(null);
            if (order != null) {
                BigDecimal paid = (order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO)
                        .subtract(req.getReturnAmount() != null ? req.getReturnAmount() : BigDecimal.ZERO).max(BigDecimal.ZERO);
                order.setPaidAmount(paid);
                purchaseOrderRepository.save(order);
            }
        } else {
            for (ReturnRequestLineDTO line : lines) {
                InventoryMovementDTO mv = new InventoryMovementDTO();
                mv.setProductId(line.getProductId());
                mv.setWarehouseId(wid);
                mv.setQuantity(line.getQuantity());
                inventoryService.addStock(mv);
            }
            SalesOrder order = salesOrderRepository.findById(req.getOrderId()).orElse(null);
            if (order != null) {
                BigDecimal recv = (order.getReceivedAmount() != null ? order.getReceivedAmount() : BigDecimal.ZERO)
                        .subtract(req.getReturnAmount() != null ? req.getReturnAmount() : BigDecimal.ZERO).max(BigDecimal.ZERO);
                order.setReceivedAmount(recv);
                salesOrderRepository.save(order);
            }
        }

        req.setStatus(ReturnFinanceRequest.Status.APPROVED);
        return toDTO(returnRepo.save(req));
    }

    @Transactional
    public ReturnRequestDTO reject(Long requestId, String reason) {
        ReturnFinanceRequest req = getRequest(requestId);
        if (req.getStatus() != ReturnFinanceRequest.Status.PENDING) {
            throw new BusinessException("仅待审批的申请可拒绝");
        }
        req.setStatus(ReturnFinanceRequest.Status.REJECTED);
        req.setRejectReason(reason);
        return toDTO(returnRepo.save(req));
    }

    public Page<ReturnRequestDTO> getRequests(String kind, String status, Pageable pageable) {
        return returnRepo.findAll((root, query, cb) -> {
            List<javax.persistence.criteria.Predicate> ps = new ArrayList<>();
            if (kind != null && !kind.isBlank()) {
                ps.add(cb.equal(root.get("kind"), ReturnFinanceRequest.Kind.valueOf(kind.toUpperCase())));
            }
            if (status != null && !status.isBlank()) {
                ps.add(cb.equal(root.get("status"), ReturnFinanceRequest.Status.valueOf(status.toUpperCase())));
            }
            return cb.and(ps.toArray(new javax.persistence.criteria.Predicate[0]));
        }, pageable).map(this::toDTO);
    }

    private ReturnRequestDTO toDTO(ReturnFinanceRequest req) {
        return ReturnRequestDTO.from(req, parseLines(req.getLinesJson()));
    }

    private List<ReturnRequestLineDTO> parseLines(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<ReturnRequestLineDTO>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(List<ReturnRequestLineDTO> lines) {
        try {
            return objectMapper.writeValueAsString(lines);
        } catch (JsonProcessingException e) {
            throw new BusinessException("序列化退货明细失败");
        }
    }

    private ReturnFinanceRequest getRequest(Long id) {
        return returnRepo.findById(id).orElseThrow(() -> new BusinessException("退货申请不存在"));
    }

    private PurchaseOrderItem findPurchaseItem(PurchaseOrder order, Long productId) {
        return order.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst().orElseThrow(() -> new BusinessException("订单中无商品 id=" + productId));
    }

    private SalesOrderItem findSalesItem(SalesOrder order, Long productId) {
        return order.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst().orElseThrow(() -> new BusinessException("订单中无商品 id=" + productId));
    }
}
