package com.trade.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trade.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 平台管理员仅可访问租户管理、个人资料等接口，禁止调用业务 API。
 */
@Component
@RequiredArgsConstructor
public class PlatformAdminAccessFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> ALLOWED_PATTERNS = List.of(
            "/auth/me",
            "/auth/me/password",
            "/admin/**",
            "/users/me"
    );

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!SecurityUtils.isPlatformAdmin()) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        if ("GET".equalsIgnoreCase(request.getMethod()) && MATCHER.match("/site-settings", path)) {
            filterChain.doFilter(request, response);
            return;
        }
        for (String pattern : ALLOWED_PATTERNS) {
            if (MATCHER.match(pattern, path)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(403, "平台管理员无权访问该功能"));
    }
}
