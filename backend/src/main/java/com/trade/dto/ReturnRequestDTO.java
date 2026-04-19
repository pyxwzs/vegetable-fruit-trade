package com.trade.dto;

import com.trade.entity.ReturnFinanceRequest;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 退货申请响应 DTO（避免懒加载序列化问题） */
@Data
public class ReturnRequestDTO {
    private Long id;
    private String kind;
    private Long orderId;
    private String orderNo;
    private Long warehouseId;
    private List<ReturnRequestLineDTO> lines;
    private String status;
    private BigDecimal returnAmount;

    private String submitUsername;
    private LocalDateTime createdAt;

    private String whApproveUsername;
    private LocalDateTime whApprovedAt;
    private String whRejectReason;

    private String finApproveUsername;
    private LocalDateTime finApprovedAt;
    private String finRejectReason;

    /** 所有字段均来自冗余列，不访问任何懒加载关联，彻底避免 LazyInitializationException */
    public static ReturnRequestDTO from(ReturnFinanceRequest req, List<ReturnRequestLineDTO> parsedLines) {
        ReturnRequestDTO dto = new ReturnRequestDTO();
        dto.setId(req.getId());
        dto.setKind(req.getKind() != null ? req.getKind().name() : null);
        dto.setOrderId(req.getOrderId());
        dto.setOrderNo(req.getOrderNo());
        dto.setWarehouseId(req.getWarehouseId());
        dto.setLines(parsedLines);
        dto.setStatus(req.getStatus() != null ? req.getStatus().name() : null);
        dto.setReturnAmount(req.getReturnAmount());
        dto.setSubmitUsername(req.getSubmitUsername());
        dto.setCreatedAt(req.getCreatedAt());
        dto.setWhApproveUsername(req.getWhApproveUsername());
        dto.setWhApprovedAt(req.getWhApprovedAt());
        dto.setWhRejectReason(req.getWhRejectReason());
        dto.setFinApproveUsername(req.getFinApproveUsername());
        dto.setFinApprovedAt(req.getFinApprovedAt());
        dto.setFinRejectReason(req.getFinRejectReason());
        return dto;
    }
}
