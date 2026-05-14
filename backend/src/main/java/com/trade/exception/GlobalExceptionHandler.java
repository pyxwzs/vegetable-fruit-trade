package com.trade.exception;

import com.trade.util.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBindException(BindException e) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return ApiResponse.error(400, message);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Void> handleBadCredentialsException(BadCredentialsException e) {
        return ApiResponse.error(401, "用户名或密码错误");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException e) {
        return ApiResponse.error(403, "没有权限执行此操作");
    }

    /**
     * 删除/更新时违反数据库外键、唯一约束等，转换为可读提示（兜底；业务层仍应优先主动校验）。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        String detail = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        log.warn("数据完整性约束: {}", detail);
        return ApiResponse.error(400, translateDataIntegrityMessage(detail));
    }

    private static String translateDataIntegrityMessage(String raw) {
        if (raw == null || raw.isBlank()) {
            return "该数据存在关联记录，无法删除或修改。请先处理关联业务，或改用停用等方式。";
        }
        String m = raw;
        if (m.contains("Duplicate entry") || m.contains("Unique index") || m.contains("UNIQUE")) {
            return "数据已存在或违反唯一约束（如编码、条码重复），请修改后重试。";
        }
        if (m.contains("product_id") && m.contains("inventories")) {
            return "该商品仍存在库存记录，无法删除。请先处理库存或改用「停用」。";
        }
        if (m.contains("product_id") && m.contains("purchase_order_items")) {
            return "该商品已被采购订单引用，无法删除。可改用「停用」。";
        }
        if (m.contains("product_id") && m.contains("sales_order_items")) {
            return "该商品已被销售订单引用，无法删除。可改用「停用」。";
        }
        if (m.contains("customer_id") && m.contains("sales_orders")) {
            return "该客户已关联销售订单，无法删除。可将客户设为停用。";
        }
        if (m.contains("supplier_id") && m.contains("purchase_orders")) {
            return "该供应商已关联采购订单，无法删除。可将供应商设为停用。";
        }
        if (m.contains("category_id") && m.contains("products")) {
            return "该分类下仍有商品，无法删除。";
        }
        if (m.contains("product_price_history") && m.contains("product_id")) {
            return "该商品存在价格变更记录等关联数据，无法删除。";
        }
        if (m.contains("Cannot delete or update a parent row") || m.contains("foreign key constraint fails")) {
            return "该数据存在关联记录，无法删除。请先解除关联或改用停用等非删除方式。";
        }
        return "该数据存在关联记录，无法删除或修改。请先处理关联业务，或改用停用等方式。";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error(500, "系统繁忙，请稍后重试");
    }
}
