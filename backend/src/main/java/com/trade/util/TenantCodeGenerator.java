package com.trade.util;

import com.trade.exception.BusinessException;
import com.trade.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class TenantCodeGenerator {

    private static final String CODE_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TenantRepository tenantRepository;

    public String generateUniqueCode() {
        for (int attempt = 0; attempt < 30; attempt++) {
            StringBuilder sb = new StringBuilder(9);
            sb.append('t');
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!"default".equals(code) && !tenantRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new BusinessException("生成租户编号失败，请重试");
    }
}
