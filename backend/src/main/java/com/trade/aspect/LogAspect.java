package com.trade.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.entity.OperationLog;
import com.trade.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {
    private final LogService logService;
    private final ObjectMapper objectMapper;

    @Pointcut("@annotation(com.trade.aspect.Loggable)")
    public void loggableMethods() {
    }

    @Pointcut("execution(* com.trade.controller.*.*(..))")
    public void controllerMethods() {
    }

    @Around("loggableMethods() || controllerMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        OperationLog operationLog = new OperationLog();

        try {
            // 获取请求信息
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                operationLog.setIp(getClientIp(request));
                operationLog.setRequestUrl(request.getRequestURI());
                operationLog.setMethod(request.getMethod());
                operationLog.setUserAgent(request.getHeader("User-Agent"));
            }

            // 获取用户信息
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            operationLog.setUsername(username);

            // 获取方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            operationLog.setModule(getModuleName(method));
            operationLog.setAction(method.getName());

            // 记录参数
            String parameters = Arrays.stream(joinPoint.getArgs())
                    .map(this::safeJsonForLog)
                    .collect(Collectors.joining(", "));
            operationLog.setParameters(parameters);

            // 执行方法
            Object result = joinPoint.proceed();

            // 记录结果（避免对含懒加载代理的返回值做 Jackson 序列化导致 500）
            operationLog.setSuccess(true);
            operationLog.setResult(safeJsonForLog(result));
            operationLog.setExecutionTime(System.currentTimeMillis() - start);

            return result;

        } catch (Exception e) {
            operationLog.setSuccess(false);
            operationLog.setErrorMessage(e.getMessage());
            operationLog.setExecutionTime(System.currentTimeMillis() - start);
            throw e;

        } finally {
            // 异步保存日志
            try {
                logService.saveLog(operationLog);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String safeJsonForLog(Object o) {
        if (o == null) {
            return "null";
        }
        if (o instanceof ResponseEntity) {
            ResponseEntity<?> re = (ResponseEntity<?>) o;
            if (re.getBody() instanceof Resource) {
                return "ResponseEntity(" + re.getStatusCode() + ",resource)";
            }
        }
        if (o instanceof Resource) {
            return "Resource(" + o.getClass().getSimpleName() + ")";
        }
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return o.getClass().getSimpleName() + "(未序列化:" + e.getClass().getSimpleName() + ")";
        }
    }

    private String getModuleName(Method method) {
        Loggable loggable = method.getAnnotation(Loggable.class);
        if (loggable != null) {
            return loggable.module();
        }
        return method.getDeclaringClass().getSimpleName().replace("Controller", "");
    }
}