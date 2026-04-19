package com.trade.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.dto.*;
import com.trade.entity.*;
import com.trade.exception.BusinessException;
import com.trade.repository.*;
import com.trade.security.SecurityUtils;
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
public class ReturnRequestService {

    private final ReturnFinanceRequestRepository returnRepo;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final InventoryService inventoryService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    // ────────────────────────────────────────────────────────────
    // 提交
    // ────────────────────────────────────────────────────────────

    /** 采购员提交采购退货申请 → PENDING */
    @Transactional
    public ReturnRequestDTO submitPurchaseReturn(Long orderId, PurchaseReturnDTO dto) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("采购订单不存在"));

        if (order.getStatus() != PurchaseOrder.OrderStatus.RECEIVED
                && order.getStatus() != PurchaseOrder.OrderStatus.COMPLETED) {
            throw new BusinessException("仅已收货或已完成的订单可申请退货");
        }

        checkNoPendingRequest(orderId, ReturnFinanceRequest.Kind.PURCHASE);

        List<ReturnRequestLineDTO> lines = new ArrayList<>();
        BigDecimal returnAmount = BigDecimal.ZERO;

        for (PurchaseReturnDTO.PurchaseReturnLineDTO line : dto.getLines()) {
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;
            PurchaseOrderItem item = findPurchaseItem(order, line.getProductId());

            BigDecimal ret = item.getReturnedQuantity() != null ? item.getReturnedQuantity() : BigDecimal.ZERO;
            BigDecimal canReturn = item.getQuantity().subtract(ret);
            if (line.getQuantity().compareTo(canReturn) > 0) {
                throw new BusinessException("商品「" + item.getProduct().getName()
                        + "」退货数量超过可退数量（可退 " + canReturn + "）");
            }

            lines.add(new ReturnRequestLineDTO(
                    line.getProductId(),
                    item.getProduct().getName(),
                    line.getQuantity(),
                    item.getPrice()));
            returnAmount = returnAmount.add(line.getQuantity().multiply(item.getPrice()));
        }

        if (lines.isEmpty()) throw new BusinessException("退货明细不能为空");

        ReturnFinanceRequest req = buildRequest(
                ReturnFinanceRequest.Kind.PURCHASE, orderId, order.getOrderNo(),
                dto.getWarehouseId() != null ? dto.getWarehouseId() : 1L,
                lines, returnAmount);
        return toDTO(returnRepo.save(req));
    }

    /** 销售员提交销售退货申请 → PENDING */
    @Transactional
    public ReturnRequestDTO submitSalesReturn(Long orderId, SalesReturnDTO dto) {
        SalesOrder order = salesOrderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("销售订单不存在"));

        if (order.getStatus() != SalesOrder.OrderStatus.SHIPPED
                && order.getStatus() != SalesOrder.OrderStatus.DELIVERED
                && order.getStatus() != SalesOrder.OrderStatus.COMPLETED) {
            throw new BusinessException("仅已发货、已送达或已完成的订单可申请退货");
        }

        checkNoPendingRequest(orderId, ReturnFinanceRequest.Kind.SALES);

        List<ReturnRequestLineDTO> lines = new ArrayList<>();
        BigDecimal returnAmount = BigDecimal.ZERO;

        for (SalesReturnDTO.SalesReturnLineDTO line : dto.getLines()) {
            if (line.getQuantity() == null || line.getQuantity().compareTo(BigDecimal.ZERO) <= 0) continue;
            SalesOrderItem item = findSalesItem(order, line.getProductId());

            BigDecimal ret = item.getReturnedQuantity() != null ? item.getReturnedQuantity() : BigDecimal.ZERO;
            BigDecimal canReturn = item.getQuantity().subtract(ret);
            if (line.getQuantity().compareTo(canReturn) > 0) {
                throw new BusinessException("商品「" + item.getProduct().getName()
                        + "」退货数量超过可退数量（可退 " + canReturn + "）");
            }

            lines.add(new ReturnRequestLineDTO(
                    line.getProductId(),
                    item.getProduct().getName(),
                    line.getQuantity(),
                    item.getPrice()));
            returnAmount = returnAmount.add(line.getQuantity().multiply(item.getPrice()));
        }

        if (lines.isEmpty()) throw new BusinessException("退货明细不能为空");

        ReturnFinanceRequest req = buildRequest(
                ReturnFinanceRequest.Kind.SALES, orderId, order.getOrderNo(),
                dto.getWarehouseId() != null ? dto.getWarehouseId() : 1L,
                lines, returnAmount);
        return toDTO(returnRepo.save(req));
    }

    // ────────────────────────────────────────────────────────────
    // 仓库审批
    // ────────────────────────────────────────────────────────────

    /** 仓管员审批通过 → 执行库存变动 → WH_APPROVED */
    @Transactional
    public ReturnRequestDTO warehouseApprove(Long requestId) {
        ReturnFinanceRequest req = getRequest(requestId);
        if (req.getStatus() != ReturnFinanceRequest.Status.PENDING) {
            throw new BusinessException("仅[待仓库审批]状态的申请可进行仓库审批");
        }

        List<ReturnRequestLineDTO> lines = parseLines(req.getLinesJson());

        if (req.getKind() == ReturnFinanceRequest.Kind.PURCHASE) {
            PurchaseOrder order = purchaseOrderRepository.findById(req.getOrderId())
                    .orElseThrow(() -> new BusinessException("采购订单不存在"));
            for (ReturnRequestLineDTO line : lines) {
                // 退货：从仓库扣减库存（货物归还给供应商）
                InventoryMovementDTO mv = new InventoryMovementDTO();
                mv.setProductId(line.getProductId());
                mv.setWarehouseId(req.getWarehouseId());
                mv.setBatchNo(order.getOrderNo());
                mv.setQuantity(line.getQuantity());
                inventoryService.removeStock(mv);
                // 更新已退货数量
                findPurchaseItem(order, line.getProductId()).setReturnedQuantity(
                        addReturned(findPurchaseItem(order, line.getProductId()).getReturnedQuantity(),
                                line.getQuantity()));
            }
            purchaseOrderRepository.save(order);

        } else {
            SalesOrder order = salesOrderRepository.findById(req.getOrderId())
                    .orElseThrow(() -> new BusinessException("销售订单不存在"));
            String batchNo = order.getOrderNo() + "-SR";
            for (ReturnRequestLineDTO line : lines) {
                // 退货：入库（货物从客户退回仓库）
                InventoryMovementDTO mv = new InventoryMovementDTO();
                mv.setProductId(line.getProductId());
                mv.setWarehouseId(req.getWarehouseId());
                mv.setBatchNo(batchNo);
                mv.setQuantity(line.getQuantity());
                mv.setPrice(line.getPrice());
                mv.setProductionDate(LocalDate.now());
                inventoryService.addStock(mv);
                findSalesItem(order, line.getProductId()).setReturnedQuantity(
                        addReturned(findSalesItem(order, line.getProductId()).getReturnedQuantity(),
                                line.getQuantity()));
            }
            salesOrderRepository.save(order);
        }

        User whUser = userService.getCurrentUser();
        req.setStatus(ReturnFinanceRequest.Status.WH_APPROVED);
        req.setWhApproveUser(whUser);
        req.setWhApproveUsername(whUser.getUsername());
        req.setWhApprovedAt(LocalDateTime.now());
        return toDTO(returnRepo.save(req));
    }

    /** 仓管员拒绝 → WH_REJECTED */
    @Transactional
    public ReturnRequestDTO warehouseReject(Long requestId, String reason) {
        ReturnFinanceRequest req = getRequest(requestId);
        if (req.getStatus() != ReturnFinanceRequest.Status.PENDING) {
            throw new BusinessException("仅[待仓库审批]状态的申请可进行仓库审批");
        }
        User whRejectUser = userService.getCurrentUser();
        req.setStatus(ReturnFinanceRequest.Status.WH_REJECTED);
        req.setWhApproveUser(whRejectUser);
        req.setWhApproveUsername(whRejectUser.getUsername());
        req.setWhApprovedAt(LocalDateTime.now());
        req.setWhRejectReason(reason);
        return toDTO(returnRepo.save(req));
    }

    // ────────────────────────────────────────────────────────────
    // 财务审批
    // ────────────────────────────────────────────────────────────

    /** 财务审批通过 → 调整已付/已收金额 → FIN_APPROVED */
    @Transactional
    public ReturnRequestDTO financeApprove(Long requestId) {
        ReturnFinanceRequest req = getRequest(requestId);
        if (req.getStatus() != ReturnFinanceRequest.Status.WH_APPROVED) {
            throw new BusinessException("仅[仓库已审批]状态的申请可进行财务审批");
        }

        BigDecimal returnAmt = req.getReturnAmount() != null ? req.getReturnAmount() : BigDecimal.ZERO;

        if (req.getKind() == ReturnFinanceRequest.Kind.PURCHASE) {
            PurchaseOrder order = purchaseOrderRepository.findById(req.getOrderId())
                    .orElseThrow(() -> new BusinessException("采购订单不存在"));
            BigDecimal paid = order.getPaidAmount() != null ? order.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal newPaid = paid.subtract(returnAmt).max(BigDecimal.ZERO);
            order.setPaidAmount(newPaid);
            order.setPaymentStatus(calcPurchasePayStatus(newPaid, order.getTotalAmount()));
            purchaseOrderRepository.save(order);

        } else {
            SalesOrder order = salesOrderRepository.findById(req.getOrderId())
                    .orElseThrow(() -> new BusinessException("销售订单不存在"));
            BigDecimal recv = order.getReceivedAmount() != null ? order.getReceivedAmount() : BigDecimal.ZERO;
            BigDecimal newRecv = recv.subtract(returnAmt).max(BigDecimal.ZERO);
            order.setReceivedAmount(newRecv);
            order.setPaymentStatus(calcSalesPayStatus(newRecv, order.getTotalAmount()));
            salesOrderRepository.save(order);
        }

        User finUser = userService.getCurrentUser();
        req.setStatus(ReturnFinanceRequest.Status.FIN_APPROVED);
        req.setFinApproveUser(finUser);
        req.setFinApproveUsername(finUser.getUsername());
        req.setFinApprovedAt(LocalDateTime.now());
        return toDTO(returnRepo.save(req));
    }

    /** 财务拒绝 → FIN_REJECTED（库存变动已发生，金额不调整） */
    @Transactional
    public ReturnRequestDTO financeReject(Long requestId, String reason) {
        ReturnFinanceRequest req = getRequest(requestId);
        if (req.getStatus() != ReturnFinanceRequest.Status.WH_APPROVED) {
            throw new BusinessException("仅[仓库已审批]状态的申请可进行财务审批");
        }
        User finRejectUser = userService.getCurrentUser();
        req.setStatus(ReturnFinanceRequest.Status.FIN_REJECTED);
        req.setFinApproveUser(finRejectUser);
        req.setFinApproveUsername(finRejectUser.getUsername());
        req.setFinApprovedAt(LocalDateTime.now());
        req.setFinRejectReason(reason);
        return toDTO(returnRepo.save(req));
    }

    // ────────────────────────────────────────────────────────────
    // 查询
    // ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ReturnRequestDTO> getRequests(
            String kind, String status, Pageable pageable) {

        boolean isAdmin       = SecurityUtils.hasAuthority("user:manage");
        boolean isWarehouse   = SecurityUtils.hasAuthority("inventory:inbound")
                             || SecurityUtils.hasAuthority("inventory:outbound");
        boolean isFinance     = SecurityUtils.hasAuthority("purchase:pay")
                             || SecurityUtils.hasAuthority("sales:collect");
        boolean canSubmit     = SecurityUtils.hasAuthority("purchase:create")
                             || SecurityUtils.hasAuthority("purchase:approve")
                             || SecurityUtils.hasAuthority("sales:create")
                             || SecurityUtils.hasAuthority("sales:approve");
        String me = SecurityUtils.currentUsername();

        Specification<ReturnFinanceRequest> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            // 可见范围
            if (!isAdmin) {
                List<Predicate> vis = new ArrayList<>();
                if (canSubmit) {
                    vis.add(cb.equal(root.get("submitUsername"), me));
                }
                if (isWarehouse) {
                    vis.add(root.get("status").in(
                            ReturnFinanceRequest.Status.PENDING,
                            ReturnFinanceRequest.Status.WH_APPROVED,
                            ReturnFinanceRequest.Status.WH_REJECTED));
                }
                if (isFinance) {
                    vis.add(root.get("status").in(
                            ReturnFinanceRequest.Status.WH_APPROVED,
                            ReturnFinanceRequest.Status.FIN_APPROVED,
                            ReturnFinanceRequest.Status.FIN_REJECTED));
                }
                if (vis.isEmpty()) {
                    return cb.disjunction(); // 无权限：返回空
                }
                ps.add(cb.or(vis.toArray(new Predicate[0])));
            }

            if (kind != null && !kind.isBlank()) {
                ps.add(cb.equal(root.get("kind"),
                        ReturnFinanceRequest.Kind.valueOf(kind.toUpperCase())));
            }
            if (status != null && !status.isBlank()) {
                ps.add(cb.equal(root.get("status"),
                        ReturnFinanceRequest.Status.valueOf(status.toUpperCase())));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        return returnRepo.findAll(spec, pageable).map(this::toDTO);
    }

    // ────────────────────────────────────────────────────────────
    // 内部工具
    // ────────────────────────────────────────────────────────────

    private ReturnFinanceRequest buildRequest(
            ReturnFinanceRequest.Kind kind, Long orderId, String orderNo,
            Long warehouseId, List<ReturnRequestLineDTO> lines, BigDecimal returnAmount) {

        User submitUser = userService.getCurrentUser();
        ReturnFinanceRequest req = new ReturnFinanceRequest();
        req.setKind(kind);
        req.setOrderId(orderId);
        req.setOrderNo(orderNo);
        req.setWarehouseId(warehouseId);
        req.setReturnAmount(returnAmount);
        req.setStatus(ReturnFinanceRequest.Status.PENDING);
        req.setSubmitUser(submitUser);
        req.setSubmitUsername(submitUser.getUsername());
        try {
            req.setLinesJson(objectMapper.writeValueAsString(lines));
        } catch (JsonProcessingException e) {
            throw new BusinessException("序列化退货明细失败");
        }
        return req;
    }

    public ReturnRequestDTO toDTO(ReturnFinanceRequest req) {
        return ReturnRequestDTO.from(req, parseLines(req.getLinesJson()));
    }

    private List<ReturnRequestLineDTO> parseLines(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<ReturnRequestLineDTO>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private ReturnFinanceRequest getRequest(Long id) {
        return returnRepo.findById(id)
                .orElseThrow(() -> new BusinessException("退货申请不存在"));
    }

    private void checkNoPendingRequest(Long orderId, ReturnFinanceRequest.Kind kind) {
        if (returnRepo.existsByOrderIdAndKindAndStatusIn(orderId, kind,
                List.of(ReturnFinanceRequest.Status.PENDING,
                        ReturnFinanceRequest.Status.WH_APPROVED))) {
            throw new BusinessException("该订单已有进行中的退货申请，请等待当前申请处理完毕");
        }
    }

    private PurchaseOrderItem findPurchaseItem(PurchaseOrder order, Long productId) {
        return order.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("订单中无商品 id=" + productId));
    }

    private SalesOrderItem findSalesItem(SalesOrder order, Long productId) {
        return order.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new BusinessException("订单中无商品 id=" + productId));
    }

    private BigDecimal addReturned(BigDecimal existing, BigDecimal qty) {
        return (existing != null ? existing : BigDecimal.ZERO).add(qty);
    }

    private PurchaseOrder.PaymentStatus calcPurchasePayStatus(BigDecimal paid, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0
                || paid.compareTo(BigDecimal.ZERO) == 0) return PurchaseOrder.PaymentStatus.UNPAID;
        if (paid.compareTo(total) >= 0) return PurchaseOrder.PaymentStatus.PAID;
        return PurchaseOrder.PaymentStatus.PARTIAL;
    }

    private SalesOrder.PaymentStatus calcSalesPayStatus(BigDecimal recv, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0
                || recv.compareTo(BigDecimal.ZERO) == 0) return SalesOrder.PaymentStatus.UNPAID;
        if (recv.compareTo(total) >= 0) return SalesOrder.PaymentStatus.PAID;
        return SalesOrder.PaymentStatus.PARTIAL;
    }
}
