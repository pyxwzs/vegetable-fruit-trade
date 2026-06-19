package com.trade.dto;

import com.trade.entity.ReturnFinanceRequest;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReturnRequestDTO {
    private Long id;
    private String kind;
    private Long orderId;
    private String orderNo;
    private List<ReturnRequestLineDTO> lines;
    private String status;
    private BigDecimal returnAmount;
    private String rejectReason;
    private LocalDateTime createdAt;

    public static ReturnRequestDTO from(ReturnFinanceRequest req, List<ReturnRequestLineDTO> parsedLines) {
        ReturnRequestDTO dto = new ReturnRequestDTO();
        dto.setId(req.getId());
        dto.setKind(req.getKind() != null ? req.getKind().name() : null);
        dto.setOrderId(req.getOrderId());
        dto.setOrderNo(req.getOrderNo());
        dto.setLines(parsedLines);
        dto.setStatus(req.getStatus() != null ? req.getStatus().name() : null);
        dto.setReturnAmount(req.getReturnAmount());
        dto.setRejectReason(req.getRejectReason());
        dto.setCreatedAt(req.getCreatedAt());
        return dto;
    }
}
